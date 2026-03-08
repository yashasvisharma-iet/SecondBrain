# AIML services

Contains two Python services used by SecondBrain.

## 1) Embeddings/relations service (FastAPI)
Computes semantic relationships between text chunks using OpenAI embeddings.

### Run
```bash
cd aiml
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8001
```

API:
- `GET /health`
- `POST /relations`
- `POST /embeddings`

## 2) AI bot service (Flask + LangGraph)
Runs a LangGraph workflow backed by OpenAI chat model credentials and optional retrieval context from the backend `/api/graph/ask` endpoint.

### Environment
Set your OpenAI key:
```bash
export OPENAI_API_KEY=<your_key>
# optional:
export OPENAI_CHAT_MODEL=gpt-4o-mini
# optional (for retrieval from backend GraphService):
export BACKEND_ASK_URL=http://localhost:8080/api/graph/ask
```

### Run
```bash
cd aiml
python ai_bot.py
```

API:
- `GET /health`
- `POST /chat`
  - Input: `{ "message": "..." }`
  - Output: `{ "answer": "...", "citations": [...], "source": "..." }`.
  - If backend returns `answer`, `/chat` returns it directly.
  - `source` is one of: `retrieval_answer`, `retrieval_citations`, `llm_fallback`.
  - If backend returns citations without `answer`, `/chat` now returns a deterministic citation-based response (no generic LLM rewrite).
  - Default behavior (`CHAT_REQUIRE_RETRIEVAL=true`): if backend retrieval does not return an answer/citation, `/chat` returns `502` with backend diagnostics instead of hallucinating an LLM-only answer.
  - Optional fallback (`CHAT_REQUIRE_RETRIEVAL=false`): when retrieval is empty/unreachable, `/chat` uses the LLM path with whatever context is available.
