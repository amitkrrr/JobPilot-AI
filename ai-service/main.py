import os
import json
import uvicorn
from fastapi import FastAPI, HTTPException
from schemas import (ParseRequest, ResumeProfile,
                     ParseJDRequest, ParsedJobRequirement, IntentRequest, SearchIntent)
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


if __name__ == "__main__":
    port = int(os.getenv("PORT", 8000))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
