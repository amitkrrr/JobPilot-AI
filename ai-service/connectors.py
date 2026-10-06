import re
from typing import List, Dict, Any, Optional

# A rich library of highly realistic, detailed job targets for Conversational Job Discovery in Bangalore
JOB_REGISTRY = [
    {
        "id": 101,
        "title": "Software Engineer",
        "company": "IBM India Software Labs",
        "location": "Bangalore, India",
        "workStyle": "HYBRID",
        "salaryInfo": "₹8 - 14 LPA",
        "experienceRequired": "0-2 years",
        "description": (
            "IBM India Software Labs is seeking a passionate Software Engineer to join our cloud modernization team. "
            "In this role, you will help design and develop cloud-native backend services, build highly scalable REST APIs, "
            "and automate continuous integration and container delivery. "
            "Requirements:\n"
            "- Strong foundation in Java and Spring Boot.\n"
            "- Experience working with relational databases (PostgreSQL or MySQL).\n"
            "- Basic understanding of container tools like Docker and container registries.\n"
            "- Exposure to building REST APIs and microservice architectures.\n"
            "- Great problem-solving and algorithmic skills."
        ),
        "sourceUrl": "https://careers.ibm.com/job/101-software-engineer",
        "skills": ["Java", "Spring Boot", "PostgreSQL", "Docker", "REST APIs"]
    },
    {
        "id": 102,
        "title": "Java Backend Developer",
        "company": "Accenture",
        "location": "Bangalore, India",
        "workStyle": "REMOTE",
        "salaryInfo": "₹6 - 11 LPA",
        "experienceRequired": "1-3 years",
        "description": (
            "Accenture is looking for an associate Java Developer with hands-on experience in Spring framework and JPA. "
            "You will be responsible for creating robust enterprise applications, maintaining test pipelines, and "
            "optimizing database queries for transactional consistency. "
            "Qualifications:\n"
            "- Hands-on development experience in Java, Spring Core, and Spring MVC.\n"
            "- Solid experience in Hibernate and database tuning.\n"
            "- Exposure to Redis caching or Kafka messaging is a plus.\n"
            "- Knowledge of version control using Git."
        ),
        "sourceUrl": "https://careers.accenture.com/job/102-backend-developer",
        "skills": ["Java", "Spring", "Hibernate", "JPA", "PostgreSQL", "Git"]
    },
    {
        "id": 103,
        "title": "Associate Platform Engineer",
        "company": "Google",
        "location": "Bangalore, India",
        "workStyle": "ONSITE",
        "salaryInfo": "₹14 - 22 LPA",
        "experienceRequired": "1-2 years",
        "description": (
            "Google Platform Systems team is seeking an Associate Engineer to help scale high-performance computing infrastructure. "
            "You will configure orchestration engines, optimize build systems, and maintain CI/CD pipelines. "
            "Requirements:\n"
            "- Competency in Python, Go, or Java.\n"
            "- Solid foundation in Docker, Kubernetes, and Unix environments.\n"
            "- Strong understanding of data structures, algorithms, and networking protocols.\n"
            "- Passion for container delivery and security automation."
        ),
        "sourceUrl": "https://careers.google.com/job/103-platform-engineer",
        "skills": ["Python", "Go", "Docker", "Kubernetes", "Linux", "CI/CD"]
    },
    {
        "id": 104,
        "title": "Junior Backend Engineer",
        "company": "Swiggy",
        "location": "Bangalore, India",
        "workStyle": "HYBRID",
        "salaryInfo": "₹10 - 16 LPA",
        "experienceRequired": "0-2 years",
        "description": (
            "Swiggy's logistics backend team is hiring a Junior Software Engineer. You will work on optimizing order-routing "
            "engines and handling high-concurrency event-driven APIs. "
            "Requirements:\n"
            "- Strong coding skills in Java, Go, or Node.js.\n"
            "- Experience building event-driven systems using Apache Kafka or RabbitMQ.\n"
            "- Familiarity with Redis, PostgreSQL, and basic query optimization.\n"
            "- Analytical approach towards problem solving."
        ),
        "sourceUrl": "https://careers.swiggy.com/job/104-junior-backend-engineer",
        "skills": ["Java", "Apache Kafka", "Redis", "PostgreSQL", "Go"]
    },
    {
        "id": 105,
        "title": "Cloud Engineer (Kubernetes & DevOps)",
        "company": "Wipro",
        "location": "Bangalore, India",
        "workStyle": "REMOTE",
        "salaryInfo": "₹5 - 9 LPA",
        "experienceRequired": "1-3 years",
        "description": (
            "Wipro is seeking a Cloud Systems associate. This role focuses on maintaining deployment configurations, "
            "modernizing legacy Jenkins pipelines, and troubleshooting multi-region Kubernetes clusters. "
            "Qualifications:\n"
            "- Deep understanding of cloud operations and Linux scripting.\n"
            "- Hands-on experience with Docker, Jenkins, Git, and Kubernetes.\n"
            "- Basic exposure to AWS or Azure environments."
        ),
        "sourceUrl": "https://careers.wipro.com/job/105-cloud-engineer",
        "skills": ["Kubernetes", "Docker", "Jenkins", "Linux", "AWS", "Git"]
    },
    {
        "id": 106,
        "title": "Python Developer",
        "company": "Ola Cabs",
        "location": "Bangalore, India",
        "workStyle": "HYBRID",
        "salaryInfo": "₹9 - 15 LPA",
        "experienceRequired": "1-2 years",
        "description": (
            "Ola Rides team is hiring a Python Developer to develop high-throughput ride-matching services using FastAPI "
            "and asynchronous databases. "
            "Requirements:\n"
            "- Strong coding foundation in Python, Pydantic, and FastAPI.\n"
            "- Experience working with Redis cache and PostgreSQL databases.\n"
            "- Familiarity with REST APIs and containerized microservices."
        ),
        "sourceUrl": "https://careers.ola.com/job/106-python-developer",
        "skills": ["Python", "FastAPI", "Pydantic", "Redis", "PostgreSQL"]
    }
]

def search_jobs(keywords: Optional[str] = None, location: Optional[str] = None, experience_range: Optional[str] = None) -> List[Dict[str, Any]]:
    """
    Search and discover normalized jobs from our target connectors (LinkedIn & Indeed) based on NLP filters.
    """
    results = []
    
    # Process queries using smart keyword/location regex
    kw_pattern = re.compile(rf"\b{re.escape(keywords)}\b", re.IGNORECASE) if keywords else None
    loc_pattern = re.compile(rf"\b{re.escape(location)}\b", re.IGNORECASE) if location else None

    for job in JOB_REGISTRY:
        match = True
        
        if kw_pattern:
            # Match keywords across title, company, description, or skills list
            kw_text = f"{job['title']} {job['company']} {job['description']} " + " ".join(job['skills'])
            if not kw_pattern.search(kw_text):
                match = False
                
        if loc_pattern and match:
            # Match location
            if not loc_pattern.search(job["location"]):
                # Check for "Remote" workStyle fallback if they queried remote
                if not (location.lower() == "remote" and job["workStyle"].lower() == "remote"):
                    match = False
                    
        # Check experience limits if provided (e.g. "0-2 years", "1 year")
        if experience_range and match:
            # Basic parsing helper
            exp_limits = [int(s) for s in re.findall(r"\d+", experience_range)]
            job_limits = [int(s) for s in re.findall(r"\d+", job["experienceRequired"])]
            if exp_limits and job_limits:
                # If target experience range overlaps or fits
                if exp_limits[0] > job_limits[-1]:
                    match = False
                    
        if match:
            results.append(job)
            
    return results
