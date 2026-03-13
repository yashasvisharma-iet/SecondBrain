# Second Brain

Second Brain is a personal knowledge system that brings notes from multiple tools into one place, then connects them into an explorable graph so users can find ideas faster, revisit context, and chat with their own knowledge.

> This README focuses on architecture/design decisions and practical setup. It is intentionally concise on product pitch and detailed on implementation choices.

## Why this architecture

Second Brain tackles three problems:
- **Fragmentation**: notes live across Notion, Google Docs, and messaging apps.
- **Low recall**: ideas are captured once and rarely revisited.
- **Weak linkage**: related notes are hard to discover when they are siloed.

To solve this, the system separates responsibilities into independently deployable services:

- **Frontend (Vite + React)** for search, graph exploration, and chat UI.
- **Backend (Spring Boot)** for ingestion orchestration, OAuth integrations, graph APIs, and persistence.
- **AIML services (Python)** for embeddings/relations and chat orchestration.
- **PostgreSQL** for durable relational storage.
- **Pinecone (optional but recommended)** for semantic retrieval at scale.

This split keeps ingestion, graph logic, and AI concerns loosely coupled so each can evolve without blocking the others.

## Design decisions and tradeoffs

### 1) Polyglot services instead of a single monolith
- **Decision**: Java/Spring for core APIs + Python for ML-focused services.
- **Why**: Spring is strong for OAuth, transactional data flows, and structured APIs; Python provides a faster iteration loop for embedding/chat workflows.
- **Tradeoff**: More operational complexity (service boundaries, env wiring, cross-service observability).

### 2) Multiple data stores by access pattern
- **Decision**: Use PostgreSQL for system-of-record data and a vector index (Pinecone) for semantic search.
- **Why**: Relational integrity is needed for users/tokens/chunks metadata; vector similarity is needed for semantic retrieval.
- **Tradeoff**: Consistency is eventual between DB records and vector index updates.

### 3) Precompute chunks + embeddings during ingestion
- **Decision**: Parse and chunk notes up front, then persist embeddings.
- **Why**: Improves query latency during graph/chat interactions and enables proactive relation scoring.
- **Tradeoff**: Ingestion pipeline is heavier; re-embedding is required when models/chunking strategy change.

### 4) Retrieval-first chat behavior
- **Decision**: Chat service can enforce retrieval-grounded answers (`CHAT_REQUIRE_RETRIEVAL=true` by default).
- **Why**: Minimizes hallucinations and keeps responses tied to ingested notes.
- **Tradeoff**: Chat may refuse to answer when retrieval is unavailable or sparse.

### 5) Docker Compose as the default local topology
- **Decision**: Run frontend, backend, AIML services, and Postgres via `docker-compose`.
- **Why**: Reproducible local setup with near-production service boundaries.
- **Tradeoff**: Slower feedback loop than running a single service natively.

## High-level data flow

1. User signs in with OAuth provider(s) from the frontend.
2. Backend receives tokens/callbacks and triggers ingestion for source notes.
3. Ingestion pipeline normalizes note content and chunk metadata.
4. AIML embeddings service computes vectors and semantic relation signals.
5. Backend persists note/chunk metadata in PostgreSQL and pushes vectors to Pinecone (if configured).
6. Graph service exposes connected-note structures for graph visualization and traversal.
7. Chat service performs retrieval over note context and returns grounded answers/citations.

## Repository layout

```text
.
├── frontend/          # React + Vite UI
├── backend/demo/      # Spring Boot API (ingestion, graph, OAuth, retrieval endpoints)
├── aiml/              # Python AIML services (embeddings + chat)
├── docker-compose.yml # Local multi-service topology
└── DEPLOYMENT.md      # Deployment-oriented notes
```

## Setup

### Prerequisites
- Docker + Docker Compose
- (Optional for local non-Docker runs) Java 17+, Node 20+, Python 3.10+
- API credentials for providers you plan to use:
  - `OPENAI_API_KEY`
  - Google OAuth client credentials
  - Notion app credentials
  - Pinecone credentials (optional but recommended for semantic retrieval)

### 1) Configure environment

Create a `.env` file in the repository root (Compose reads these automatically):

```bash
# Core
POSTGRES_DB=secondbrain
POSTGRES_USER=secondbrain
POSTGRES_PASSWORD=secondbrain

# Frontend
VITE_API_BASE_URL=https://your-backend.onrender.com  # or omit to use current browser origin
VITE_NOTION_CLIENT_ID=<notion_client_id>
VITE_NOTION_REDIRECT_URI=https://your-frontend.onrender.com/auth/notion/callback

# Backend OAuth + CORS
# IMPORTANT: set this to your FRONTEND origin(s), not the backend API URL.
# Example (Render): APP_CORS_ALLOWED_ORIGINS=https://your-frontend.onrender.com
APP_CORS_ALLOWED_ORIGINS=https://your-frontend.onrender.com
APP_FRONTEND_GOOGLE_CALLBACK_URL=https://your-frontend.onrender.com/auth/google/callback
GOOGLE_CLIENT_ID=<google_client_id>
GOOGLE_CLIENT_SECRET=<google_client_secret>
GOOGLE_REDIRECT_URI=https://your-backend.onrender.com/login/oauth2/code/google

# AIML
OPENAI_API_KEY=<openai_key>
OPENAI_CHAT_MODEL=gpt-4o-mini
CHAT_REQUIRE_RETRIEVAL=true

# Vector store (optional)
PINECONE_API_KEY=<pinecone_api_key>
PINECONE_INDEX_HOST=<pinecone_index_host>
PINECONE_NAMESPACE=secondbrain
VECTOR_STORE_DIMENSIONS=1536
SEMANTIC_THRESHOLD=0.45
SEMANTIC_TOP_K=8
```

### 2) Start the full stack

```bash
docker compose up --build
```

Default endpoints:
- Frontend: `http://localhost`
- Backend: `http://localhost:8080` (inside compose network, frontend proxies/calls this base)
- AIML Embeddings: `http://localhost:8001`
- AIML Chat: `http://localhost:8002`
- PostgreSQL: `localhost:5432`

### 3) Validate health

```bash
# Embeddings service
curl http://localhost:8001/health

# Chat service
curl http://localhost:8002/health
```

For backend/frontend validation, use the app UI and ingestion/chat endpoints once OAuth credentials are set.

## Running services individually (optional)

### Frontend
```bash
cd frontend
npm install
npm run dev
```

### Backend
```bash
cd backend/demo
./mvnw spring-boot:run
```

### AIML embeddings
```bash
cd aiml
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8001
```

### AIML chat
```bash
cd aiml
source .venv/bin/activate
python ai_bot.py
```

## Non-goals (v1)

- Team workspaces, shared permissions, and collaborative editing.
- Broad connector ecosystem beyond initial sources.
- Strong real-time consistency guarantees across all external integrations.

## Future design evolution

- Incremental sync with change tracking per source connector.
- Background re-index/re-embed jobs with versioned embeddings.
- Explainable edge creation (why two notes are connected).
- Personal memory lanes (time-based resurfacing and revision prompts).

## Additional docs

- `DEPLOYMENT.md` for deployment details.
- `aiml/README.md` for AIML API specifics.
