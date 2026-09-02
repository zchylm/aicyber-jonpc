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

## Local PostgreSQL

The backend reads its database connection from environment variables. Copy the values from
`.env.example` into the IntelliJ run configuration or your shell; do not commit a real password.

```text
JON_PC_DB_URL=jdbc:postgresql://localhost:5432/jonpc
JON_PC_DB_USERNAME=postgres
JON_PC_DB_PASSWORD=your-local-password
```

At this stage the PostgreSQL driver and connection pool are configured, but no application tables
are created yet. The next step is to add the first persistence slice for users and build requests.
