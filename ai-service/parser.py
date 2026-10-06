import re
import os
import io
import json
import time
import requests
import boto3
from pypdf import PdfReader
from typing import List, Dict, Any, Optional
from dotenv import load_dotenv
from schemas import ResumeProfile, ParsedJobRequirement

# Load local environment variables from .env file
load_dotenv()

# Local fallback configuration properties
MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "http://localhost:9000")
AWS_ACCESS_KEY = os.getenv("AWS_ACCESS_KEY", "jobpilot-access-key")
AWS_SECRET_KEY = os.getenv("AWS_SECRET_KEY", "jobpilot-secret-key")
AWS_REGION = os.getenv("AWS_REGION", "us-east-1")
BUCKET_NAME = os.getenv("BUCKET_NAME", "jobpilot-resumes")

# Robust technical skills library for keyword extraction
SKILLS_LIBRARY = [
    "java", "spring boot", "spring security", "hibernate", "jpa", "postgresql", "mysql",
    "react", "typescript", "javascript", "html", "css", "tailwind", "vite", "zustand", "redux",
    "python", "fastapi", "django", "flask", "pydantic", "langgraph", "langchain", "embeddings",
    "docker", "podman", "kubernetes", "kafka", "redis", "minio", "s3", "maven", "git", "jenkins",
    "aws", "gcp", "azure", "ci/cd", "rest api", "graphql", "microservices"
]

def get_s3_client():
    return boto3.client(
        "s3",
        endpoint_url=MINIO_ENDPOINT,
        aws_access_key_id=AWS_ACCESS_KEY,
        aws_secret_access_key=AWS_SECRET_KEY,
        region_name=AWS_REGION,
        config=boto3.session.Config(signature_version="s3v4")
    )

def extract_text_from_pdf_bytes(pdf_bytes: bytes) -> str:
    """Helper method to extract clean text from PDF binary stream."""
    try:
        reader = PdfReader(io.BytesIO(pdf_bytes))
        text = ""
        for page in reader.pages:
            page_text = page.extract_text()
            if page_text:
                text += page_text + "\n"
        return text
    except Exception as e:
        print(f"Error extracting text from PDF: {e}")
        return ""

INVALID_HOSTS = ["localhost", "127.0.0.1", "0.0.0.0"]

def _normalize_url(url: str) -> Optional[str]:
    """Ensure the URL has an https:// scheme and is not a localhost address."""
    if not url:
        return None
    url = url.strip().rstrip("/")
    if any(h in url.lower() for h in INVALID_HOSTS):
        return None
    if not url.startswith("http"):
        url = f"https://{url}"
    return url

def extract_hyperlinks_from_pdf(pdf_bytes: bytes) -> Dict[str, Optional[str]]:
    """
    Walk every page's annotation layer and pull out URI actions.
    Returns a dict with keys linkedin_url, github_url, portfolio_url
    (first matching URL wins for each category).
    """
    links: Dict[str, Optional[str]] = {
        "linkedin_url": None,
        "github_url": None,
        "portfolio_url": None,
    }
    try:
        reader = PdfReader(io.BytesIO(pdf_bytes))
        for page in reader.pages:
            annots = page.get("/Annots")
            if not annots:
                continue
            for annot_ref in annots:
                try:
                    annot = annot_ref.get_object() if hasattr(annot_ref, "get_object") else annot_ref
                    a_obj = annot.get("/A")
                    if not a_obj:
                        continue
                    a = a_obj.get_object() if hasattr(a_obj, "get_object") else a_obj
                    subtype = a.get("/S")
                    if str(subtype) != "/URI":
                        continue
                    uri = str(a.get("/URI", "")).strip()
                    if not uri:
                        continue
                    uri_lower = uri.lower()
                    if "linkedin.com" in uri_lower and not links["linkedin_url"]:
                        links["linkedin_url"] = _normalize_url(uri)
                    elif "github.com" in uri_lower and not links["github_url"]:
                        links["github_url"] = _normalize_url(uri)
                    elif (
                        not links["portfolio_url"]
                        and not any(b in uri_lower for b in ["linkedin.com", "github.com", "gmail.com", "outlook.com", "yahoo.com", "hotmail.com", "mailto:", "localhost"])
                        and uri.startswith("http")
                    ):
                        links["portfolio_url"] = _normalize_url(uri)
                except Exception:
                    continue
    except Exception as e:
        print(f"Warning: could not extract hyperlinks from PDF: {e}")
    return links

def deterministic_parser(text: str) -> Dict[str, Any]:
    """Parses raw text using advanced regex and section-aware heuristics matching our persona."""
    profile: Dict[str, Any] = {
        "name": None,
        "email": None,
        "phone": None,
        "location": None,
        "headline": None,
        "years_of_experience": 0,
        "current_company": None,
        "current_role": None,
        "linkedin_url": None,
        "github_url": None,
        "portfolio_url": None,
        "skills": [],
        "education": [],
        "experience": []
    }

    # 1. Extract Email
    email_match = re.search(r"[\w\.-]+@[\w\.-]+\.\w+", text)
    if email_match:
        profile["email"] = email_match.group(0).strip()

    # 2. Extract Phone
    phone_match = re.search(r"(\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}", text)
    if phone_match:
        profile["phone"] = phone_match.group(0).strip()

    # 2.1 Extract LinkedIn URL
    linkedin_match = re.search(r"(https?://)?(www\.)?linkedin\.com/in/[A-Za-z0-9_-]+/?", text, re.IGNORECASE)
    if linkedin_match:
        raw = linkedin_match.group(0).strip()
        profile["linkedin_url"] = raw if raw.startswith("http") else f"https://{raw}"

    # 2.2 Extract GitHub URL
    github_match = re.search(r"(https?://)?(www\.)?github\.com/[A-Za-z0-9_-]+/?", text, re.IGNORECASE)
    if github_match:
        raw = github_match.group(0).strip()
        profile["github_url"] = raw if raw.startswith("http") else f"https://{raw}"

    # 2.3 Extract Portfolio URL (Generic websites excluding linkedin, github, gmail, outlook, localhost, etc.)
    portfolio_matches = re.finditer(r"https?://[a-zA-Z0-9][a-zA-Z0-9_-]*(?:\.[a-zA-Z]{2,6})+(?:/[a-zA-Z0-9._~:/?#[\]@!$&'()*+,;=%-]*)?", text)
    for p_match in portfolio_matches:
        url = p_match.group(0).strip()
        url_lower = url.lower()
        if not any(blocked in url_lower for blocked in ["linkedin.com", "github.com", "gmail.com", "outlook.com", "yahoo.com", "hotmail.com", "localhost", "pdf", "resume"]):
            profile["portfolio_url"] = url
            break

    # 3. Extract Name
    lines = [line.strip() for line in text.split("\n") if line.strip()]
    if lines:
        first_line = lines[0]
        if first_line.lower().startswith("for resume"):
            name_match = re.search(r"for resume\s+([A-Za-z\s]+)", first_line, re.IGNORECASE)
            if name_match:
                profile["name"] = name_match.group(1).strip()
            else:
                profile["name"] = first_line.replace("for resume", "").strip()[:100]
        else:
            profile["name"] = first_line[:100]

    # 4. Extract Location (looks for "City, State/Country" or fallback to country)
    text_lower = text.lower()
    location_match = re.search(r"([A-Za-z\s\.\,]+,\s*(?:India|USA|UK|US|Canada|Germany|Singapore|Allahabad))", text, re.IGNORECASE)
    if location_match:
        profile["location"] = location_match.group(1).strip()
    else:
        if "india" in text_lower:
            profile["location"] = "India"

    # 5. Extract Skills using our robust library
    for skill in SKILLS_LIBRARY:
        if re.search(r"\b" + re.escape(skill) + r"\b", text_lower):
            profile["skills"].append(skill.title() if len(skill) > 3 else skill.upper())

    # 6. Extract Education (Simple line-matching heuristics)
    for line in lines:
        if any(keyword in line.lower() for keyword in ["bachelor", "master", "degree", "university", "college", "b.tech", "m.tech", "b.sc", "m.sc"]):
            profile["education"].append(line)

    # 7. Extract Section-Aware Professional Experience
    # Identify index of the "Experience" header to avoid matching words in general "Summary"
    experience_index = -1
    for idx, line in enumerate(lines):
        if line.lower() in ["experience", "work experience", "professional experience"]:
            experience_index = idx
            break

    role_found = False
    if experience_index != -1:
        # Scan lines directly below "Experience"
        for idx in range(experience_index + 1, min(experience_index + 20, len(lines))):
            line = lines[idx]
            
            # Role titles are short and contain target keywords
            if any(keyword in line.lower() for keyword in ["engineer", "developer", "architect", "analyst", "lead", "manager"]):
                # Filter out long descriptive summaries or lists
                if len(line) < 65 and not any(k in line.lower() for k in ["experience in", "summary", "passionate", "built", "implemented", "overview"]):
                    
                    # Clean bullets and date suffixes from the role line
                    cleaned_role = re.sub(r"^[\s•◦*◦\-]+", "", line).strip()
                    # Strip dates (e.g. July 2025 - Present)
                    cleaned_role = re.sub(r"\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|July|Present|Full Time|Part Time|Intern).*$", "", cleaned_role, flags=re.IGNORECASE).strip()
                    # Strip trailing hyphens or symbols
                    cleaned_role = re.sub(r"[-–—\s/]+$", "", cleaned_role).strip()
                    
                    profile["current_role"] = cleaned_role
                    role_found = True
                    
                    # Company is typically on the line directly ABOVE
                    line_above = lines[idx - 1] if idx > experience_index + 1 else ""
                    if line_above and len(line_above) < 55:
                        cleaned_company = re.sub(r"^[\s•◦*◦\-]+", "", line_above).strip()
                        # Strip trailing location tags from company line
                        cleaned_company = re.sub(r"\b(?:India|Labs India|Labs)\b.*$", "Labs", cleaned_company, flags=re.IGNORECASE).strip()
                        profile["current_company"] = cleaned_company
                    break

    # General Fallback if no specific Experience header was found
    if not role_found:
        for line in lines[:30]:
            if any(keyword in line.lower() for keyword in ["engineer", "developer", "architect", "analyst", "lead", "manager"]):
                if len(line) < 65 and not any(k in line.lower() for k in ["experience in", "summary", "passionate", "built", "implemented", "overview"]):
                    cleaned_role = re.sub(r"^[\s•◦*◦\-]+", "", line).strip()
                    profile["current_role"] = cleaned_role
                    
                    company_match = re.search(r"\b(?:at|@|for|in)\s+([A-Za-z0-9\s\.\,\-]+)", line, re.IGNORECASE)
                    if company_match:
                        profile["current_company"] = company_match.group(1).strip()
                    break

    # Synthesize clean professional headline
    if profile["current_role"] and profile["current_company"]:
        profile["headline"] = f"{profile['current_role']} at {profile['current_company']}"
    elif profile["current_role"]:
        profile["headline"] = profile["current_role"]

    # 8. Extract Years of Experience
    exp_matches = re.findall(r"(\d+)\+?\s*years?\s+of\s+experience", text_lower)
    if exp_matches:
        profile["years_of_experience"] = max(int(val) for val in exp_matches)
    else:
        # Fallback to realistic junior/mid parameter if they have active jobs
        profile["years_of_experience"] = 1

    return profile

def _gemini_post(url: str, headers: dict, payload: dict, retries: int = 3) -> Optional[requests.Response]:
    """POST to Gemini with exponential backoff on 503 Service Unavailable."""
    delay = 3
    for attempt in range(retries):
        try:
            response = requests.post(url, headers=headers, json=payload, timeout=20)
            if response.status_code == 503:
                print(f"Gemini 503 overload (attempt {attempt + 1}/{retries}), retrying in {delay}s...")
                time.sleep(delay)
                delay *= 2
                continue
            return response
        except Exception as e:
            print(f"Gemini request error (attempt {attempt + 1}/{retries}): {e}")
            time.sleep(delay)
            delay *= 2
    return None

def llm_parser(text: str) -> Optional[Dict[str, Any]]:
    """Uses Google's free Gemini API with forced JSON Schema to parse resume text perfectly."""
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        print("GEMINI_API_KEY not configured. Falling back to deterministic NLP parser.")
        return None

    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key={api_key}"
    headers = {"Content-Type": "application/json"}
    
    # Prompt instructing Gemini on its parsing role
    prompt = (
        "You are an elite, highly accurate ATS resume parser. Analyze the raw text of the candidate's resume "
        "and extract the required structural attributes. Extract the candidate's name, email, phone, location, "
        "skills, education details, and work experience. "
        "Identify their current company and current role under their professional experience. "
        "Locate and extract their professional links: LinkedIn URL, GitHub URL, and personal portfolio URL if present. "
        "Generate a professional, clean professional headline like 'Software Engineer at IBM India Software Labs'. "
        "Ensure the output conforms exactly to the requested JSON schema. Do not make up or fabricate achievements, "
        "employment, or skills. Base your extraction purely on the actual raw resume text.\n\n"
        f"RAW RESUME TEXT:\n{text}"
    )
    
    # Payload specifying forced JSON response conforming to our Pydantic schema
    payload = {
        "contents": [{
            "parts": [{"text": prompt}]
        }],
        "generationConfig": {
            "responseMimeType": "application/json",
            "responseSchema": {
                "type": "OBJECT",
                "properties": {
                    "name": {"type": "STRING"},
                    "email": {"type": "STRING"},
                    "phone": {"type": "STRING"},
                    "location": {"type": "STRING"},
                    "headline": {"type": "STRING"},
                    "years_of_experience": {"type": "INTEGER"},
                    "current_company": {"type": "STRING"},
                    "current_role": {"type": "STRING"},
                    "linkedin_url": {"type": "STRING"},
                    "github_url": {"type": "STRING"},
                    "portfolio_url": {"type": "STRING"},
                    "skills": {"type": "ARRAY", "items": {"type": "STRING"}},
                    "education": {"type": "ARRAY", "items": {"type": "STRING"}},
                    "experience": {"type": "ARRAY", "items": {"type": "STRING"}}
                },
                "required": ["name", "email", "phone", "location", "headline", "years_of_experience", "current_company", "current_role", "skills"]
            }
        }
    }
    
    response = _gemini_post(url, headers, payload)
    if response is None:
        print("Gemini API unreachable after retries.")
        return None
    if response.status_code == 200:
        try:
            candidates = response.json().get("candidates", [])
            if candidates:
                parts = candidates[0].get("content", {}).get("parts", [])
                if parts:
                    return json.loads(parts[0].get("text", ""))
        except Exception as e:
            print(f"Failed to parse Gemini response: {e}")
    else:
        print(f"Gemini API returned status code {response.status_code}: {response.text}")
    return None

def parse_resume_file(s3_key: str) -> ResumeProfile:
    """Downloads resume from MinIO, extracts text, and parses into ResumeProfile schema."""
    s3_client = get_s3_client()
    
    try:
        # Download raw S3 object bytes
        response = s3_client.get_object(Bucket=BUCKET_NAME, Key=s3_key)
        file_bytes = response["Body"].read()
    except Exception as e:
        raise ValueError(f"Failed to fetch resume file from MinIO bucket '{BUCKET_NAME}': {e}")

    # Step 1: Extract embedded hyperlinks from PDF annotation layer (most reliable source)
    pdf_links = extract_hyperlinks_from_pdf(file_bytes)
    print(f"PDF hyperlinks found: {pdf_links}")

    # Step 2: Extract plain text from PDF
    raw_text = extract_text_from_pdf_bytes(file_bytes)
    if not raw_text:
        raise ValueError("Could not extract any readable text from the uploaded PDF document.")

    # Step 3: Run text-based parser (LLM preferred, deterministic fallback)
    parsed_data = llm_parser(raw_text)
    if parsed_data:
        print("Success: Resume successfully parsed using Google Gemini LLM!")
    else:
        print("Falling back to deterministic parser.")
        parsed_data = deterministic_parser(raw_text)

    # Step 4: Merge — PDF hyperlinks take priority; sanitize any localhost values from text parser
    for url_field in ("linkedin_url", "github_url", "portfolio_url"):
        if pdf_links.get(url_field):
            # Embedded hyperlink is authoritative
            parsed_data[url_field] = pdf_links[url_field]
        else:
            # Sanitize text-parsed value: drop localhost, normalize scheme
            parsed_data[url_field] = _normalize_url(parsed_data.get(url_field) or "")

    return ResumeProfile(**parsed_data)

def deterministic_jd_parser(text: str) -> Dict[str, Any]:
    """Offline fallback parser for job descriptions using section-aware keyword matching."""
    text_lower = text.lower()
    req = {
        "required_skills": [],
        "preferred_skills": [],
        "minimum_experience": 0,
        "required_education": "Bachelor's Degree",
        "responsibilities": []
    }
    
    # 1. Match skills from SKILLS_LIBRARY
    found_skills = []
    for skill in SKILLS_LIBRARY:
        if re.search(r"\b" + re.escape(skill) + r"\b", text_lower):
            found_skills.append(skill.title() if len(skill) > 3 else skill.upper())
    
    # Partition them: split first half as required, second half as preferred
    if found_skills:
        half = max(1, len(found_skills) // 2 + 1)
        req["required_skills"] = found_skills[:half]
        req["preferred_skills"] = found_skills[half:]
        
    # 2. Match minimum experience
    exp_matches = re.findall(r"(\d+)\+?\s*years?\s+of\s+experience", text_lower)
    if exp_matches:
        req["minimum_experience"] = min(int(val) for val in exp_matches)
    else:
        # Check other experience phrases
        exp_match2 = re.search(r"(\d+)\+?\s*years?", text_lower)
        if exp_match2:
            req["minimum_experience"] = int(exp_match2.group(1))
        else:
            req["minimum_experience"] = 2  # standard mid-level fallback
            
    # 3. Required education
    if "master" in text_lower or "m.tech" in text_lower or "m.s." in text_lower:
        req["required_education"] = "Master's Degree"
    elif "phd" in text_lower or "ph.d" in text_lower:
        req["required_education"] = "PhD"
    else:
        req["required_education"] = "Bachelor's Degree"
        
    # 4. Responsibilities: extract bullet points or sentences that start with action verbs
    lines = [line.strip() for line in text.split("\n") if line.strip()]
    responsibilities = []
    action_verbs = ["develop", "design", "build", "write", "lead", "collaborate", "implement", "create", "manage", "maintain", "test", "support"]
    for line in lines:
        cleaned = re.sub(r"^[\s•◦*◦\-]+", "", line).strip()
        if cleaned:
            cleaned_lower = cleaned.lower()
            if any(cleaned_lower.startswith(verb) for verb in action_verbs) or len(cleaned) < 80 and "responsibility" in cleaned_lower:
                if len(cleaned) > 15 and len(cleaned) < 120:
                    responsibilities.append(cleaned)
                    
    if not responsibilities:
        # Fallback to some sentences
        responsibilities = [line for line in lines if len(line) > 30 and len(line) < 100][:4]
        
    req["responsibilities"] = responsibilities
    return req

def llm_jd_parser(text: str) -> Optional[Dict[str, Any]]:
    """Uses Google's free Gemini API with forced JSON Schema to parse Job Description text perfectly."""
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        print("GEMINI_API_KEY not configured. Falling back to deterministic JD parser.")
        return None

    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key={api_key}"
    headers = {"Content-Type": "application/json"}
    
    prompt = (
        "You are an elite, highly accurate ATS Job Description parser. Analyze the raw text of the job description "
        "and extract the required structural attributes. Extract: \n"
        "1. required_skills: mandatory core technical skills and certifications (e.g. ['Java', 'Spring Boot'])\n"
        "2. preferred_skills: nice-to-have technologies, frameworks, and tools (e.g. ['Kubernetes', 'AWS'])\n"
        "3. minimum_experience: minimum numeric years of experience required as an integer. Convert words like 'five years' to 5. If no experience is mentioned, return 0.\n"
        "4. required_education: minimum educational degree or certification required (e.g. 'Bachelor's Degree' or 'Master's Degree')\n"
        "5. responsibilities: key duties and core responsibilities of the role as a list of bullet points.\n\n"
        "Ensure the output conforms exactly to the requested JSON schema. Do not invent details. Base your extraction purely on the actual raw job description text.\n\n"
        f"RAW JOB DESCRIPTION TEXT:\n{text}"
    )
    
    payload = {
        "contents": [{
            "parts": [{"text": prompt}]
        }],
        "generationConfig": {
            "responseMimeType": "application/json",
            "responseSchema": {
                "type": "OBJECT",
                "properties": {
                    "required_skills": {"type": "ARRAY", "items": {"type": "STRING"}},
                    "preferred_skills": {"type": "ARRAY", "items": {"type": "STRING"}},
                    "minimum_experience": {"type": "INTEGER"},
                    "required_education": {"type": "STRING"},
                    "responsibilities": {"type": "ARRAY", "items": {"type": "STRING"}}
                },
                "required": ["required_skills", "preferred_skills", "minimum_experience", "required_education", "responsibilities"]
            }
        }
    }
    
    response = _gemini_post(url, headers, payload)
    if response is None:
        print("Gemini API unreachable after retries.")
        return None
    if response.status_code == 200:
        try:
            candidates = response.json().get("candidates", [])
            if candidates:
                parts = candidates[0].get("content", {}).get("parts", [])
                if parts:
                    return json.loads(parts[0].get("text", ""))
        except Exception as e:
            print(f"Failed to parse Gemini JD response: {e}")
    else:
        print(f"Gemini API returned status code {response.status_code}: {response.text}")
    return None

def parse_job_description(description: str) -> ParsedJobRequirement:
    """Parses a job description text using LLM with deterministic fallback."""
    parsed_data = llm_jd_parser(description)
    if parsed_data:
        print("Success: Job description successfully parsed using Google Gemini LLM!")
    else:
        print("Falling back to deterministic Job Description parser.")
        parsed_data = deterministic_jd_parser(description)
        
    return ParsedJobRequirement(**parsed_data)
