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
  - Output: `{ "answer": "...", "citations": [...] }`
