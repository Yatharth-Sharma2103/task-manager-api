# Task Manager API — Spring Boot

A production-style REST API for managing tasks with **secure JWT authentication**, **per-user task isolation**, and **AI/ML-powered task management** (smart prioritization, deadline estimation, and a natural-language task assistant).

The application runs **out of the box** on an in-memory H2 database (no setup required) and can be switched to **PostgreSQL** or **MySQL** with a single profile flag.

---

## Tech Stack

| Layer | Technology |
|------------------------|--------------------------------------------------------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 (Spring MVC) |
| Security | Spring Security + JWT (JJWT 0.12) |
| Persistence | Spring Data JPA / Hibernate 6 |
| Databases | H2 (default/dev), PostgreSQL, MySQL |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| API Docs | springdoc-openapi (Swagger UI) |
| Scheduling | Spring `@Scheduled` (deadline reminders) |
| AI/ML | Pluggable engine: offline heuristic/NLP **or** OpenAI |
| Build | Maven |

---

## Core Features

- **User Authentication & Authorization** — register/login with BCrypt-hashed passwords; stateless auth via signed **JWT** Bearer tokens. Role-based (`ROLE_USER` / `ROLE_ADMIN`) and method-level security ready.
- **CRUD Operations for Tasks** — standard REST endpoints built with Spring MVC + JPA, with pagination and sorting.
- **Task Visibility Based on Logged-in User** — every query is scoped to the authenticated owner, so users only ever see and modify their own tasks (verified: cross-user access returns `404`).
- **Priority Levels & Status Tagging** — enums for `Priority` (`LOW`, `MEDIUM`, `HIGH`, `URGENT`) and `Status` (`TODO`, `IN_PROGRESS`, `DONE`), with filtering support.

## AI/ML Enhancements

- **Smart Task Prioritization** — predicts a task's priority from its description. When a task is created without a priority, the AI engine fills it in automatically. `POST /api/ai/prioritize`
- **Deadline Estimation** — an NLP engine estimates completion effort and suggests a due date, honoring explicit time phrases like *"by next Friday"* or *"in 3 days"*. `POST /api/ai/estimate-deadline`
- **AI Assistant for Tasks** — send a natural-language message (e.g. *"Remind me to submit the tax report by next Friday, it's urgent"*) and the assistant extracts the title, priority, and deadline and creates the task. `POST /api/ai/assistant`

### AI providers (pluggable)

The AI layer is behind a single `AiService` interface with two interchangeable implementations:

- **`heuristic`** (default) — a fully offline keyword-scoring + rule-based NLP engine. No API keys, no network calls, always available.
- **`openai`** — calls the OpenAI Chat Completions API for higher-quality results. If the key is missing or a call fails, it **transparently falls back** to the heuristic engine so the API never breaks.

Select the provider with `app.ai.provider` (see Configuration).

## Optional Features

- **Deadline Reminders** — a `@Scheduled` job periodically scans for overdue, not-yet-done tasks and emits a reminder (logged; wire in email/push in production), marking each task so it isn't reminded twice.

---

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.9+

### Run (default: in-memory H2)

```bash
mvn spring-boot:run
```

or build and run the jar:

```bash
mvn clean package
java -jar target/task-manager-api-1.0.0.jar
```

The API starts on `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:taskdb`, user `sa`, empty password)

### Run against PostgreSQL

```bash
java -jar target/task-manager-api-1.0.0.jar \
  --spring.profiles.active=postgres \
  --DB_URL=jdbc:postgresql://localhost:5432/taskdb \
  --DB_USERNAME=postgres --DB_PASSWORD=postgres
```

### Run against MySQL

```bash
java -jar target/task-manager-api-1.0.0.jar --spring.profiles.active=mysql
```

---

## Configuration

All settings live in `src/main/resources/application.yml` and are overridable via environment variables or CLI args.

| Property | Env var | Default | Description |
|-----------------------------|----------------------|-------------------|--------------------------------------------|
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | `h2` | Database profile: `h2`, `postgres`, `mysql` |
| `app.jwt.secret` | `JWT_SECRET` | dev secret | HS256 signing key (**override in prod**, ≥256-bit) |
| `app.jwt.expiration-ms` | `JWT_EXPIRATION_MS` | `86400000` (24h) | Access-token lifetime |
| `app.ai.provider` | `AI_PROVIDER` | `heuristic` | `heuristic` or `openai` |
| `app.ai.openai.api-key` | `OPENAI_API_KEY` | *(empty)* | Required when provider is `openai` |
| `app.ai.openai.model` | `OPENAI_MODEL` | `gpt-4o-mini` | OpenAI model name |
| `app.reminders.enabled` | `REMINDERS_ENABLED` | `true` | Enable the scheduled reminder job |

To use OpenAI:

```bash
export AI_PROVIDER=openai
export OPENAI_API_KEY=sk-...
java -jar target/task-manager-api-1.0.0.jar
```

---

## API Reference

### Authentication (`/api/auth`) — public

| Method | Endpoint | Description |
|--------|--------------------|-----------------------------------|
| POST | `/api/auth/register` | Create a user, returns a JWT |
| POST | `/api/auth/login` | Authenticate, returns a JWT |

### Tasks (`/api/tasks`) — requires `Authorization: Bearer <token>`

| Method | Endpoint | Description |
|--------|-----------------------------|-------------------------------------------------|
| GET | `/api/tasks` | List own tasks (`?status=`, `?priority=`, paging) |
| GET | `/api/tasks/{id}` | Get one task |
| POST | `/api/tasks` | Create a task (priority AI-predicted if omitted) |
| PUT | `/api/tasks/{id}` | Update a task |
| PATCH | `/api/tasks/{id}/status` | Update only the status (`?status=DONE`) |
| DELETE | `/api/tasks/{id}` | Delete a task |

### AI (`/api/ai`) — requires JWT

| Method | Endpoint | Description |
|--------|-----------------------------|--------------------------------------|
| POST | `/api/ai/prioritize` | Predict priority from text |
| POST | `/api/ai/estimate-deadline` | Estimate effort and suggest a due date |
| POST | `/api/ai/assistant` | Create a task from natural language |

---

## Example Usage

```bash
# 1) Register (returns a JWT)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","password":"secret123"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# 2) Create a task with the AI assistant (natural language)
curl -s -X POST http://localhost:8080/api/ai/assistant \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"message":"Remind me to submit the quarterly tax report by next Friday, it is urgent"}'

# 3) Create a task without a priority -> AI predicts URGENT
curl -s -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Fix critical production bug","description":"payment service is down, urgent"}'

# 4) List your tasks
curl -s http://localhost:8080/api/tasks -H "Authorization: Bearer $TOKEN"
```

---

## Project Structure

```
src/main/java/org/example/taskmanager
├── TaskManagerApplication.java      # Entry point (@EnableScheduling)
├── config/                          # Security & OpenAPI configuration
│   ├── SecurityConfig.java
│   └── OpenApiConfig.java
├── controller/                      # REST controllers (Auth, Task, AI)
├── domain/                          # JPA entities + enums (User, Task, Priority, Status, Role)
├── dto/                             # Request/response records
├── exception/                       # Custom exceptions + global handler
├── repository/                      # Spring Data JPA repositories
├── security/                        # JWT service, filter, user details, entry point
└── service/                         # Business logic
    ├── AuthService.java
    ├── TaskService.java
    ├── AiAssistantService.java
    ├── ReminderService.java         # @Scheduled deadline reminders
    └── ai/                          # AiService + heuristic & OpenAI engines
```

---

## Security Notes

- Passwords are hashed with **BCrypt**; plaintext is never stored.
- Auth is **stateless** (no server sessions); each request carries a signed JWT.
- The default `app.jwt.secret` is for development only — always supply a strong secret via `JWT_SECRET` in production.
