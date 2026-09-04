# ReconAI — AI-Powered Financial Reconciliation Platform

ReconAI is a full-stack financial reconciliation platform that compares records from payment gateways, bank statements, and internal ledgers.

It uses a **two-pass reconciliation approach**:

1. **Pass 1 — Deterministic Rule Matching:** Java/Spring Boot matches transactions using exact IDs, amounts, dates, fees, and controlled matching rules.
2. **Pass 2 — AI-Assisted Matching:** A Python/FastAPI microservice uses the Groq API to analyze ambiguous records that could not be confidently matched by the rule engine.

The platform also provides authentication, role-based access, audit logs, financial metrics, reports, and an AI chat interface for asking questions about reconciliation batches.

---

## Key Features

- Multi-source CSV ingestion
- Payment gateway, bank statement, and internal ledger reconciliation
- Two-pass hybrid matching engine
- Deterministic rule-based matching
- AI-assisted ambiguous transaction matching
- Confidence threshold for AI decisions
- Manual review workflow
- JWT-based authentication
- BCrypt password hashing
- Role-based access control
- Batch processing and reconciliation metrics
- Audit logging
- Financial reports
- Grounded AI chat for batch-related questions
- React dashboard with charts and transaction tables
- H2 database for easy local development
- PostgreSQL support for production
- Docker support

---

## Architecture

```text
                  ┌─────────────────────┐
                  │      React UI       │
                  │ React + TypeScript  │
                  │ Tailwind + Recharts │
                  └──────────┬──────────┘
                             │
                             ▼
                  ┌─────────────────────┐
                  │   Spring Boot API   │
                  │       Java 21       │
                  │                     │
                  │ Auth / Batches      │
                  │ Reconciliation      │
                  │ Metrics / Reports   │
                  │ Audit / Chat        │
                  └───────┬───────┬─────┘
                          │       │
              ┌───────────┘       └──────────────┐
              ▼                                  ▼
     ┌─────────────────┐                ┌──────────────────┐
     │ H2 / PostgreSQL │                │ Python AI Service │
     │    Database     │                │ FastAPI + Groq    │
     └─────────────────┘                └──────────────────┘
```

### Reconciliation Flow

```text
CSV Upload
   │
   ▼
Parse Gateway / Bank / Ledger Data
   │
   ▼
Pass 1: Rule Matching
   │
   ├── High-confidence match ──► Matched
   │
   └── Ambiguous records
             │
             ▼
       Pass 2: Groq AI
             │
             ├── Confidence >= 0.70 ──► Auto Resolved
             │
             └── Confidence < 0.70 ──► Manual Review
```

---

## Technology Stack

### Backend
- Java 21
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- Spring Security
- JWT / JJWT
- BCrypt
- Apache Commons CSV
- Apache POI
- Maven

### AI Microservice
- Python 3.11+
- FastAPI
- Uvicorn
- Pydantic
- Groq SDK
- python-dotenv

### Frontend
- React 18
- TypeScript
- Vite
- Tailwind CSS
- Axios
- Recharts
- Lucide React

### Database
- H2 — local development
- PostgreSQL — production

---

## Project Structure

```text
ReconAI/
│
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/aifinance/
│       │   │   ├── config/
│       │   │   ├── controller/
│       │   │   ├── dto/
│       │   │   ├── entity/
│       │   │   ├── repository/
│       │   │   ├── security/
│       │   │   └── service/
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application-h2.properties
│       │       └── application-postgres.properties
│       └── test/
│
├── ai-service/
│   ├── main.py
│   ├── requirements.txt
│   ├── Dockerfile
│   └── services/
│       ├── groq_service.py
│       ├── matcher.py
│       ├── chat_agent.py
│       └── audit_logger.py
│
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   └── src/
│       ├── api/
│       ├── components/
│       ├── context/
│       ├── pages/
│       └── types/
│
├── data/
│   └── sample CSV data
│
├── docker-compose.yml
├── README.md
└── .gitignore
```

---

## Prerequisites

Install:

- Java 21
- Maven
- Python 3.11+
- Node.js 18+
- npm

A Groq API key is required for live AI-powered matching and chat. The AI service can also be run without the key using its configured fallback/simulation behavior.

---

## Running the Project

### 1. Start AI Service

```bash
cd ai-service

python -m venv venv
```

Windows:

```bash
venv\Scripts\activate
```

Linux/macOS:

```bash
source venv/bin/activate
```

Install dependencies:

```bash
pip install -r requirements.txt
```

Create/configure your environment variables, including:

```text
GROQ_API_KEY=your_groq_api_key
```

Start the service:

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

AI service:

```text
http://localhost:8000
```

Health check:

```text
http://localhost:8000/health
```

---

### 2. Start Spring Boot Backend

```bash
cd backend
mvn spring-boot:run
```

Backend:

```text
http://localhost:8080
```

The default configuration uses H2.

For PostgreSQL, activate the PostgreSQL profile:

```bash
SPRING_PROFILES_ACTIVE=postgres
```

Configure the required PostgreSQL environment variables according to the project's PostgreSQL configuration.

---

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

---

## Environment Variables

Do not commit secrets to Git.

Typical environment variables include:

```text
GROQ_API_KEY=your_groq_api_key
JWT_SECRET=your_secure_jwt_secret
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000
AI_SERVICE_URL=http://localhost:8000
SPRING_PROFILES_ACTIVE=h2
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Use a `.env` file locally where appropriate and keep it ignored by Git.

---

## API Services

### AI Service

| Endpoint | Method | Purpose |
|---|---|---|
| `/health` | GET | AI service health/status |
| `/models` | GET | Available/active Groq models |
| `/match-ambiguous` | POST | AI-assisted ambiguous transaction matching |
| `/chat` | POST | Financial reconciliation Q&A |
| `/audit-logs` | GET | Recent AI audit logs |

### Spring Boot Backend

The backend provides endpoints for:

- Authentication
- Batch creation and processing
- CSV upload
- Reconciliation
- Matching results
- Metrics
- Reporting
- Audit logs
- Chat
- Manual review

---

## Matching Strategy

### Pass 1 — Rule Engine

The Java backend attempts deterministic matches first.

Typical matching signals include:

- Transaction/reference ID
- Amount
- Date
- Date windows
- Fees
- Payment information
- Narration/reference text

This keeps routine reconciliation fast, predictable, and auditable.

### Pass 2 — AI Matching

Only unresolved or ambiguous records are sent to the AI service.

The AI service evaluates possible candidates and returns structured matching information with confidence.

The configured confidence threshold is:

```text
0.70
```

Low-confidence AI results are not automatically committed and can be sent for manual review.

---

## Security

ReconAI includes:

- Stateless JWT authentication
- BCrypt password hashing
- Protected API endpoints
- Role-based authorization
- Login rate limiting
- AI request rate limiting
- Configurable CORS
- Environment-based secrets
- Global exception handling

Supported application roles include:

```text
ROLE_ADMIN
ROLE_ANALYST
ROLE_VIEWER
```

---

## Testing

Backend tests are located under:

```text
backend/src/test/
```

Run tests with:

```bash
cd backend
mvn test
```

The project includes tests for areas such as:

- JWT token handling
- CSV parsing
- Rule-based matching

---

## Synthetic Data

The project can be tested with synthetic financial records containing:

- Gateway transactions
- Bank statement records
- Internal ledger records
- Missing records
- Amount discrepancies
- Fee differences
- Ambiguous reconciliation cases

This allows the complete reconciliation workflow to be tested without using real financial information.

---

## Docker

The project contains Docker configuration for containerized deployment.

Build the backend:

```bash
cd backend
docker build -t reconai-backend .
```

Build the AI service:

```bash
cd ai-service
docker build -t reconai-ai-service .
```

If using the project's Docker Compose configuration:

```bash
docker compose up --build
```

---

## Example Workflow

```text
1. Register / Login
        ↓
2. Upload Gateway CSV
        ↓
3. Upload Bank CSV
        ↓
4. Upload Internal Ledger CSV
        ↓
5. Validate the batch
        ↓
6. Run Pass 1 — Rule Matching
        ↓
7. Review unresolved records
        ↓
8. Run Pass 2 — AI Matching
        ↓
9. Auto-resolve high-confidence matches
        ↓
10. Send low-confidence records to Manual Review
        ↓
11. View metrics and reports
        ↓
12. Ask the AI assistant questions about the batch
```

---

## Important

This project is designed for demonstration, development, and synthetic-data testing. Do not commit real financial data, API keys, passwords, JWT secrets, database credentials, or other confidential information to the repository.
