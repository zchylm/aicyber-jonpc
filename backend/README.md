# JON. PC Backend

Spring Boot backend for the JON. PC website. The backend is organised around the AI chat domain so provider integrations, prompts, hardware knowledge and future compatibility tools stay separate.

```text
src/main/java/com/aicyber/backend/
├── BackendApplication.java
└── ai/
    ├── config/      Provider and environment configuration
    ├── controller/  HTTP API endpoints
    ├── dto/         Chat request and response models
    ├── knowledge/   JON. PC hardware facts and retrieval
    ├── prompt/      System prompts and prompt construction
    ├── provider/    GPT, Gemini and Claude adapters
    └── service/     Conversation orchestration and business logic
```

The intended request flow is:

```text
Frontend → Controller → Service → Knowledge / Prompt → Provider → Response
```
