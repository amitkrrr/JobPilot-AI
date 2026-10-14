# JobPilot AI

## Configuring Job Connectors

JobPilot uses two real-time job APIs to surface live listings in the AI chat. Both have **free tiers** and take about 2 minutes to set up.

### 1. Adzuna
- Sign up at **https://developer.adzuna.com**
- After registration you will get an **App ID** and an **App Key**.

### 2. Jooble
- Request a key at **https://jooble.org/api/about**
- You will receive a single **API key** by email.

---

### Local development setup

1. Open `backend/src/main/resources/application-local.properties`  
   (already created; this file is git-ignored so keys are never committed).

2. Replace the placeholder values:

   ```properties
   adzuna.app-id=YOUR_ADZUNA_APP_ID
   adzuna.app-key=YOUR_ADZUNA_APP_KEY
   jooble.api-key=YOUR_JOOBLE_API_KEY
   ```

3. Start the backend with the `local` profile:

   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

That's it. Once the keys are set, the AI chat will start returning live job results from both sources automatically.

---

### CI / Production

Set the following environment variables on your server or CI pipeline:

| Variable | Description |
|---|---|
| `ADZUNA_APP_ID` | Adzuna application ID |
| `ADZUNA_APP_KEY` | Adzuna application key |
| `JOOBLE_API_KEY` | Jooble API key |
