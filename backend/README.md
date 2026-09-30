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

## Transactional email

Customer email is written to `transactional_email_outbox` before delivery. The dispatcher uses a
stable idempotency key, retries temporary API failures, and records Resend delivery webhooks. Message
bodies, account-action links and attachments are removed from the outbox after Resend accepts them.

Current messages:

- account email verification and password reset;
- custom-build received confirmation and optional operations alert;
- reviewed custom-build quote ready;
- payment confirmation with the paid tax invoice attached.

Add delivery, cancellation/refund and cashback payout messages only when those backend lifecycle
events become authoritative. Do not infer them from a page visit or an admin preview.

Production settings:

```text
JON_PC_EMAIL_PROVIDER=resend
JON_PC_EMAIL_DISPATCH_ENABLED=true
JON_PC_EMAIL_FROM="JON. PC <support@jonpc.com.au>"
JON_PC_ORDERS_EMAIL_FROM="JON. PC Orders <orders@jonpc.com.au>"
JON_PC_EMAIL_REPLY_TO=support@jonpc.com.au
JON_PC_SUPPORT_EMAIL=support@jonpc.com.au
JON_PC_EMAIL_OPERATIONS_RECIPIENT=support@jonpc.com.au
JON_PC_RESEND_API_KEY=re_...
JON_PC_RESEND_WEBHOOK_SECRET=whsec_...
JON_PC_EMAIL_VERIFICATION_ENABLED=true
JON_PC_PASSWORD_RESET_ENABLED=true
JON_PC_INVOICE_EMAIL=support@jonpc.com.au
JON_PC_INVOICE_EMAIL_DELIVERY_ENABLED=true
```

Register `POST /api/email/webhooks/resend` in Resend for `email.sent`, `email.delivered`,
`email.delivery_delayed`, `email.failed`, `email.bounced`, `email.complained`, and
`email.suppressed`. Customer replies go to `JON_PC_EMAIL_REPLY_TO`; inbound-email ingestion is
intentionally not enabled until JON. PC has a defined support-ticket workflow.

For an intentional local end-to-end invoice test, run the backend with the `local` profile plus
`JON_PC_PAYMENTS_DEMO_ENABLED=true`, `JON_PC_EMAIL_PROVIDER=resend`,
`JON_PC_EMAIL_DISPATCH_ENABLED=true`, and `JON_PC_INVOICE_EMAIL_DELIVERY_ENABLED=true`. Supply
`JON_PC_RESEND_API_KEY` through the shell or IDE secret storage, never a committed file. Completing
the mock checkout sends a real email to the signed-in account address, so use an approved test inbox.
