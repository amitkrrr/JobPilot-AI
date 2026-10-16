import os
import io
import base64
import json
import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from schemas import (ParseRequest, ResumeProfile,
                     ParseJDRequest, ParsedJobRequirement, IntentRequest, SearchIntent,
                     MatchRequest, MatchResult,
                     ResumeOptimizeRequest, ResumeOptimizeResult, ResumeOptimizeSuggestion)
from parser import parse_resume_file, parse_job_description, _gemini_post

app = FastAPI(
    title="JobPilot AI — Python AI Service",
    description="Microservice handling natural language processing, resume parsing, and intent classification.",
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

    context_str = ""
    if request.last_intent:
        context_str = f"Previous search context: role={request.last_intent.role} skills={request.last_intent.skills} location={request.last_intent.location}. The user is refining this search.\n\n"

    prompt = (
        "You are a JSON intent classifier. Output ONLY a JSON object — no prose.\n\n"
        "TASK: Classify whether the user wants to search for jobs, or is asking a general question.\n\n"
        f"{context_str}"
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


@app.post("/api/ai/match", response_model=MatchResult)
async def score_match(request: MatchRequest):
    """
    Evaluates candidate fit against job requirements and returns match score, strengths, gaps, and verdict.
    """
    if not GEMINI_API_KEY:
        req_set = {s.lower() for s in request.required_skills} if request.required_skills else set()
        cand_set = {s.lower() for s in request.candidate_skills} if request.candidate_skills else set()
        matched = req_set.intersection(cand_set)
        missing = req_set.difference(cand_set)

        score = 50
        if req_set:
            score = int((len(matched) / len(req_set)) * 100)

        strengths = [s.title() for s in matched] if matched else ["Relevant background"]
        gaps = [s.title() for s in missing] if missing else ["None identified"]
        verdict = f"Moderate fit with {score}% skill alignment." if score >= 50 else f"Low fit with {score}% skill alignment."
        if score >= 75:
            verdict = f"Strong candidate match with {score}% skill overlap."

        return MatchResult(score=score, strengths=strengths, gaps=gaps, verdict=verdict)

    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key={GEMINI_API_KEY}"
    headers = {"Content-Type": "application/json"}

    prompt = (
        "You are an expert technical recruiter analyzing a candidate's fit for a job.\n"
        "Analyze the job requirements and candidate profile to calculate a match score from 0 to 100, "
        "identify strengths, missing skills/gaps, and provide an overall concise verdict.\n\n"
        f"JOB DESCRIPTION:\n{request.job_description}\n\n"
        f"REQUIRED SKILLS: {request.required_skills}\n"
        f"CANDIDATE SKILLS: {request.candidate_skills}\n"
        f"CANDIDATE YEARS OF EXPERIENCE: {request.years_of_experience}\n\n"
        "Return ONLY a JSON object with:\n"
        "- score: integer from 0 to 100\n"
        "- strengths: list of strings (matching skills/experience)\n"
        "- gaps: list of strings (missing skills or experience gaps)\n"
        "- verdict: concise 1-2 sentence assessment of candidate fit\n"
    )

    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "responseMimeType": "application/json",
            "responseSchema": {
                "type": "OBJECT",
                "properties": {
                    "score": {"type": "INTEGER"},
                    "strengths": {"type": "ARRAY", "items": {"type": "STRING"}},
                    "gaps": {"type": "ARRAY", "items": {"type": "STRING"}},
                    "verdict": {"type": "STRING"}
                },
                "required": ["score", "strengths", "gaps", "verdict"]
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
                    return MatchResult(**data)
        except Exception as e:
            print(f"Failed to parse match response: {e}")

    req_set = {s.lower() for s in request.required_skills} if request.required_skills else set()
    cand_set = {s.lower() for s in request.candidate_skills} if request.candidate_skills else set()
    matched = req_set.intersection(cand_set)
    missing = req_set.difference(cand_set)
    score = int((len(matched) / len(req_set)) * 100) if req_set else 60
    return MatchResult(
        score=score,
        strengths=[s.title() for s in matched] if matched else ["General experience"],
        gaps=[s.title() for s in missing] if missing else ["None"],
        verdict=f"Candidate has {score}% skill alignment with the job requirements."
    )


def _extract_text_from_pdf_b64(b64_data: str) -> str:
    """Decode a base64 PDF and extract its text using pypdf."""
    try:
        import pypdf
        pdf_bytes = base64.b64decode(b64_data)
        reader = pypdf.PdfReader(io.BytesIO(pdf_bytes))
        pages = []
        for page in reader.pages:
            text = page.extract_text()
            if text:
                pages.append(text)
        return "\n".join(pages).strip()
    except Exception as e:
        print(f"PDF text extraction failed: {e}")
        return ""


@app.post("/api/ai/optimize-resume", response_model=ResumeOptimizeResult)
async def optimize_resume(request: ResumeOptimizeRequest):
    """
    Analyzes a resume and returns actionable improvement suggestions.
    Optionally tailored to a specific job description or target role.
    Accepts either resume_pdf_b64 (base64 PDF, preferred) or resume_text.
    """
    # Resolve resume text: prefer decoded PDF bytes over plain text
    resume_text = ""
    if request.resume_pdf_b64:
        resume_text = _extract_text_from_pdf_b64(request.resume_pdf_b64)
    if not resume_text and request.resume_text:
        resume_text = request.resume_text

    if not resume_text or len(resume_text.strip()) < 50:
        return _optimize_response(ResumeOptimizeResult(
            overallScore=0,
            summary="Could not extract readable text from your resume. Please ensure your PDF is not scanned/image-based, then try again.",
            suggestions=[
                ResumeOptimizeSuggestion(
                    category="Format",
                    suggestion="Ensure your resume is a text-based PDF (not a scanned image). Re-export from Word or Google Docs.",
                    priority="HIGH"
                )
            ],
            missingKeywords=[]
        ))

    if not GEMINI_API_KEY:
        return _optimize_response(ResumeOptimizeResult(
            overallScore=70,
            summary="Resume analysis requires Gemini API key. Configure GEMINI_API_KEY to get detailed suggestions.",
            suggestions=[
                ResumeOptimizeSuggestion(
                    category="Skills",
                    suggestion="Ensure your skills section lists specific technologies relevant to your target role.",
                    priority="HIGH"
                ),
                ResumeOptimizeSuggestion(
                    category="Summary",
                    suggestion="Add a compelling professional summary at the top highlighting your key value proposition.",
                    priority="HIGH"
                )
            ],
            missingKeywords=[]
        ))

    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key={GEMINI_API_KEY}"
    headers = {"Content-Type": "application/json"}

    job_context = ""
    if request.job_description:
        job_context = f"\nTARGET JOB DESCRIPTION:\n{request.job_description[:2000]}\n"
    if request.target_role:
        job_context += f"\nTARGET ROLE: {request.target_role}\n"

    prompt = (
        "You are an expert ATS resume coach and technical recruiter.\n"
        "Analyze this resume and provide specific, actionable improvement suggestions.\n"
        "Score the resume 0-100 based on: clarity, ATS keyword density, quantified achievements, "
        "professional summary strength, skills relevance, and formatting.\n\n"
        f"RESUME TEXT:\n{resume_text[:4000]}\n"
        f"{job_context}\n"
        "Return a JSON object with:\n"
        "- overall_score: integer 0-100\n"
        "- summary: 2-3 sentence overall assessment\n"
        "- suggestions: array of objects with 'category' (Summary/Skills/Experience/Keywords/Format), "
        "'suggestion' (specific actionable text), 'priority' (HIGH/MEDIUM/LOW)\n"
        "- missing_keywords: array of important keywords/skills missing from the resume "
        "(especially relevant to the target role if provided)\n"
        "Provide 5-8 specific, actionable suggestions. Be direct and specific, not generic."
    )

    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "responseMimeType": "application/json",
            "responseSchema": {
                "type": "OBJECT",
                "properties": {
                    "overall_score": {"type": "INTEGER"},
                    "summary": {"type": "STRING"},
                    "suggestions": {
                        "type": "ARRAY",
                        "items": {
                            "type": "OBJECT",
                            "properties": {
                                "category": {"type": "STRING"},
                                "suggestion": {"type": "STRING"},
                                "priority": {"type": "STRING"}
                            },
                            "required": ["category", "suggestion", "priority"]
                        }
                    },
                    "missing_keywords": {"type": "ARRAY", "items": {"type": "STRING"}}
                },
                "required": ["overall_score", "summary", "suggestions", "missing_keywords"]
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
                    suggestions = [ResumeOptimizeSuggestion(**s) for s in data.get("suggestions", [])]
                    return _optimize_response(ResumeOptimizeResult(
                        overallScore=data.get("overall_score", 70),
                        summary=data.get("summary", ""),
                        suggestions=suggestions,
                        missingKeywords=data.get("missing_keywords", [])
                    ))
        except Exception as e:
            print(f"Failed to parse optimize-resume response: {e}")

    raise HTTPException(status_code=503, detail="Resume optimization service temporarily unavailable.")


def _optimize_response(result: ResumeOptimizeResult) -> JSONResponse:
    """Return ResumeOptimizeResult as camelCase JSON for Spring's Jackson deserializer."""
    return JSONResponse(content=result.model_dump_camel())


if __name__ == "__main__":
    port = int(os.getenv("PORT", 8000))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
