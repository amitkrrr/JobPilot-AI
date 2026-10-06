import os
import json
import requests
import uvicorn
from fastapi import FastAPI, HTTPException
from schemas import (ParseRequest, ResumeProfile, ChatRequest, AIChatResponse, JobCardResponse,
                     ParseJDRequest, ParsedJobRequirement, IntentRequest, SearchIntent)
from parser import parse_resume_file, parse_job_description, _gemini_post
from connectors import search_jobs

app = FastAPI(
    title="JobPilot AI — Python AI Service",
    description="Microservice handling natural language processing, resume parsing, and conversational job discovery.",
    version="0.1.0"
)

# Load Gemini API Key from environment
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")

@app.post("/api/ai/parse", response_model=ResumeProfile)
async def parse_resume(request: ParseRequest):
    """
    Exposes an endpoint to parse an uploaded resume from MinIO S3.
    """
    try:
        print(f"Triggering AI Parser for S3 key: {request.s3_key}")
        profile = parse_resume_file(request.s3_key)
        return profile
    except Exception as e:
        print(f"Error parsing resume: {e}")
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/api/ai/intent", response_model=SearchIntent)
async def classify_intent(request: IntentRequest):
    """
    Pure intent classification endpoint — called by Spring Boot before job search.
    Returns structured SearchIntent, never job results.
    Gemini understands natural language; Spring Boot owns job fetching.
    """
    message = request.message
    profile = request.profile
    print(f"Classifying intent for: '{message}'")

    # Deterministic fallback when no API key
    if not GEMINI_API_KEY:
        return _deterministic_intent(message)

    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key={GEMINI_API_KEY}"
    headers = {"Content-Type": "application/json"}

    prompt = (
        "You are a JSON intent classifier. Output ONLY a JSON object — no prose.\n\n"
        "TASK: Classify whether the user wants to search for jobs, or is asking a general question.\n\n"
        "OUTPUT SCHEMA:\n"
        "- intent: 'JOB_SEARCH' or 'GENERAL_CHAT'\n"
        "- role: job title or role (e.g. 'Backend Developer', 'DevOps Engineer') — null if not a search\n"
        "- skills: array of technologies/skills mentioned (e.g. ['Java', 'Spring Boot']) — empty if not a search\n"
        "- location: city or 'Remote' — null if not mentioned\n"
        "- experience_min: minimum years as integer — null if not mentioned\n"
        "- experience_max: maximum years as integer — null if not mentioned\n"
        "- remote: true if user explicitly wants remote — false otherwise\n"
        "- reply: helpful career answer ONLY when intent is GENERAL_CHAT — null for JOB_SEARCH\n\n"
        "EXAMPLES:\n"
        "'Find me Java Spring Boot jobs in Bangalore' → {\"intent\":\"JOB_SEARCH\",\"role\":\"Backend Developer\","
        "\"skills\":[\"Java\",\"Spring Boot\"],\"location\":\"Bangalore\",\"experience_min\":null,\"experience_max\":null,"
        "\"remote\":false,\"reply\":null}\n"
        "'Show junior remote Python openings' → {\"intent\":\"JOB_SEARCH\",\"role\":\"Python Developer\","
        "\"skills\":[\"Python\"],\"location\":null,\"experience_min\":0,\"experience_max\":2,\"remote\":true,\"reply\":null}\n"
        "'What is microservices architecture?' → {\"intent\":\"GENERAL_CHAT\",\"role\":null,\"skills\":[],"
        "\"location\":null,\"experience_min\":null,\"experience_max\":null,\"remote\":false,"
        "\"reply\":\"Microservices is an architectural style...\"}\n\n"
        f"USER MESSAGE: '{message}'\n"
        f"USER PROFILE: {profile.model_dump_json() if profile else 'None'}"
    )

    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "responseMimeType": "application/json",
            "responseSchema": {
                "type": "OBJECT",
                "properties": {
                    "intent": {"type": "STRING"},
                    "role": {"type": "STRING"},
                    "skills": {"type": "ARRAY", "items": {"type": "STRING"}},
                    "location": {"type": "STRING"},
                    "experience_min": {"type": "INTEGER"},
                    "experience_max": {"type": "INTEGER"},
                    "remote": {"type": "BOOLEAN"},
                    "reply": {"type": "STRING"}
                },
                "required": ["intent", "skills", "remote"]
            }
        }
    }

    response = _gemini_post(url, headers, payload)
    if response is not None and response.status_code == 200:
        try:
            candidates = response.json().get("candidates", [])
            if candidates:
                parts = candidates[0].get("content", {}).get("parts", [])
                if parts:
                    data = json.loads(parts[0].get("text", ""))
                    print(f"Intent classified: {data.get('intent')} | role={data.get('role')} | skills={data.get('skills')} | location={data.get('location')}")
                    return SearchIntent(**data)
        except Exception as e:
            print(f"Failed to parse intent response: {e}")
    else:
        if response is not None:
            print(f"Gemini intent API returned {response.status_code}: {response.text}")

    return _deterministic_intent(message)


def _deterministic_intent(message: str) -> SearchIntent:
    """Offline keyword-based fallback intent classifier."""
    msg_lower = message.lower()
    is_search = any(k in msg_lower for k in [
        "find", "search", "show", "get", "jobs", "openings", "positions",
        "hiring", "opportunities", "roles", "vacancies"
    ])
    if not is_search:
        return SearchIntent(intent="GENERAL_CHAT", skills=[], remote=False,
                            reply="Hello! I'm JobPilot. Try asking me to find jobs — e.g. 'Find me Java Spring Boot jobs in Bangalore!'")

    skills = []
    for tech in ["java", "spring boot", "python", "react", "node", "devops", "kubernetes", "aws", "go", "typescript"]:
        if tech in msg_lower:
            skills.append(tech.title())

    location = None
    for city in ["bangalore", "mumbai", "delhi", "hyderabad", "pune", "chennai", "remote"]:
        if city in msg_lower:
            location = city.title()
            break

    role = None
    for r in ["backend developer", "frontend developer", "fullstack developer", "devops engineer",
              "software engineer", "java developer", "python developer", "data engineer"]:
        if any(w in msg_lower for w in r.split()):
            role = r.title()
            break

    remote = "remote" in msg_lower
    exp_min = 0 if any(w in msg_lower for w in ["junior", "entry", "fresher", "0-2"]) else None
    exp_max = 2 if exp_min == 0 else None

    return SearchIntent(intent="JOB_SEARCH", role=role, skills=skills, location=location,
                        experience_min=exp_min, experience_max=exp_max, remote=remote)


@app.post("/api/ai/parse-jd", response_model=ParsedJobRequirement)
async def parse_jd(request: ParseJDRequest):
    """
    Exposes an endpoint to parse raw job description text.
    """
    try:
        print("Triggering AI Job Description Parser")
        requirements = parse_job_description(request.description)
        return requirements
    except Exception as e:
        print(f"Error parsing job description: {e}")
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/api/ai/chat", response_model=AIChatResponse)
async def process_chat(request: ChatRequest):
    """
    Exposes an intelligent conversational endpoint to handle job discovery search queries.
    Uses Gemini to identify search intent, query search connectors (LinkedIn/Indeed),
    and return interactive job cards.
    """
    message = request.message
    profile = request.profile
    print(f"Processing conversational chat query: '{message}'")

    if not GEMINI_API_KEY:
        # 1. LOCAL FALLBACK OFFLINE ROUTING (Deterministic Keyword Matching)
        print("GEMINI_API_KEY not configured. Running offline deterministic routing.")
        msg_lower = message.lower()
        is_search = any(k in msg_lower for k in ["find", "search", "jobs", "openings", "positions", "hiring", "opportunities"])
        
        if is_search:
            # Simple keyword matching fallbacks
            kw = "Java" if "java" in msg_lower else ("Python" if "python" in msg_lower else ("Kubernetes" if "kubernetes" in msg_lower or "devops" in msg_lower else None))
            loc = "Bangalore" if "bangalore" in msg_lower else ("Remote" if "remote" in msg_lower else None)
            exp = "0-2 years" if "0-2" in msg_lower or "junior" in msg_lower or "entry" in msg_lower else None
            
            jobs = search_jobs(keywords=kw, location=loc, experience_range=exp)
            reply = f"I've searched our connectors (LinkedIn/Indeed) and found {len(jobs)} active job postings matching your query."
            return AIChatResponse(response=reply, jobs=jobs)
        else:
            reply = "Hello! I am JobPilot, your conversational assistant. I can help you search, discover, and match jobs. To get started, try asking: 'Find me Java Spring Boot jobs in Bangalore!'"
            return AIChatResponse(response=reply, jobs=[])

    # 2. ONLINE LLM-ROUTED INTENT ANALYSIS (Gemini 3.8 Flash)
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key={GEMINI_API_KEY}"
    headers = {"Content-Type": "application/json"}
    
    # Prompt instructing Gemini to classify intent and extract search params strictly
    prompt = (
        "You are a JSON classification engine. Your ONLY job is to classify the user message and output a JSON object. "
        "DO NOT search the internet. DO NOT say whether jobs exist or not. DO NOT answer conversationally.\n\n"
        "RULE: If the message contains words like 'find', 'search', 'show', 'get', 'jobs', 'openings', 'roles', 'positions', "
        "'hiring', 'opportunities', or names any technology/role — set is_job_search=true and extract keywords/location/experience.\n\n"
        "OUTPUT SCHEMA (always return valid JSON):\n"
        "- is_job_search: boolean (true if user wants job listings, false for general questions)\n"
        "- keywords: string — tech stack or role name (e.g. 'Java Spring Boot', 'Python', 'DevOps'). Empty string if not a search.\n"
        "- location: string — city or 'Remote' (e.g. 'Bangalore'). Empty string if not mentioned.\n"
        "- experience: string — seniority if mentioned (e.g. 'junior', '2-4 years'). Empty string if not mentioned.\n"
        "- reply: string — ONLY populate this if is_job_search=false. Write a helpful career/coding answer.\n\n"
        "EXAMPLES:\n"
        "Input: 'Find me Java Spring Boot jobs in Bangalore' → {\"is_job_search\":true,\"keywords\":\"Java Spring Boot\",\"location\":\"Bangalore\",\"experience\":\"\",\"reply\":\"\"}\n"
        "Input: 'Show me remote Python jobs' → {\"is_job_search\":true,\"keywords\":\"Python\",\"location\":\"Remote\",\"experience\":\"\",\"reply\":\"\"}\n"
        "Input: 'What is Spring Boot?' → {\"is_job_search\":false,\"keywords\":\"\",\"location\":\"\",\"experience\":\"\",\"reply\":\"Spring Boot is...\"}\n\n"
        f"USER MESSAGE: '{message}'\n"
        f"USER PROFILE: {profile.model_dump_json() if profile else 'None'}"
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
                    "is_job_search": {"type": "BOOLEAN"},
                    "keywords": {"type": "STRING"},
                    "location": {"type": "STRING"},
                    "experience": {"type": "STRING"},
                    "reply": {"type": "STRING"}
                },
                "required": ["is_job_search", "keywords", "location", "experience", "reply"]
            }
        }
    }

    try:
        response = requests.post(url, headers=headers, json=payload, timeout=15)
        if response.status_code == 200:
            res_json = response.json()
            candidates = res_json.get("candidates", [])
            if candidates:
                parts = candidates[0].get("content", {}).get("parts", [])
                if parts:
                    extracted_text = parts[0].get("text", "")
                    classification = json.loads(extracted_text)
                    
                    if classification.get("is_job_search"):
                        # Execute search using our LinkedIn / Indeed connectors
                        kw = classification.get("keywords")
                        loc = classification.get("location")
                        exp = classification.get("experience")
                        print(f"Gemini matched Search Intent! Keywords: {kw}, Location: {loc}, Exp: {exp}")
                        
                        jobs = search_jobs(keywords=kw, location=loc, experience_range=exp)
                        
                        # Generate a personalized conversational greeting summarizing the results
                        greeting_prompt = (
                            f"Summarize what you found for the user. User asked: '{message}'. "
                            f"We searched Indeed & LinkedIn and found {len(jobs)} jobs matching parameters "
                            f"(Keywords: '{kw}', Location: '{loc}', Experience: '{exp}'). "
                            "Write a single sentence greeting in a friendly, professional tone (e.g. 'I've discovered "
                            "4 entry-level positions in Bangalore...'). Keep it short and encouraging."
                        )
                        greeting_payload = {
                            "contents": [{"parts": [{"text": greeting_prompt}]}]
                        }
                        greet_resp = requests.post(url, headers=headers, json=greeting_payload, timeout=10)
                        reply = "Here are the jobs I discovered for you matching your criteria:"
                        if greet_resp.status_code == 200:
                            g_json = greet_resp.json()
                            g_cand = g_json.get("candidates", [])
                            if g_cand:
                                reply = g_cand[0].get("content", {}).get("parts", [])[0].get("text", reply).strip()
                        
                        return AIChatResponse(response=reply, jobs=jobs)
                    else:
                        # Return normal conversational reply from Gemini
                        reply = classification.get("reply", "Hello! How can I assist you with your career search today?")
                        return AIChatResponse(response=reply, jobs=[])
                        
        print(f"Gemini API returned status code {response.status_code}: {response.text}")
    except Exception as e:
        print(f"Failed to query Gemini Chat API: {e}")
        
    # Safe Fallback
    return AIChatResponse(
        response="I ran into a small connection issue with my AI brain, but I'm still here! Try asking me again in a moment.",
        jobs=[]
    )

if __name__ == "__main__":
    import os
    port = int(os.getenv("PORT", 8000))
    # Run server locally on port 8000
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
