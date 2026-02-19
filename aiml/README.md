# AIML service (FastAPI)

Computes semantic relationships between text chunks using OpenAI embeddings in Python.

## Run

```bash
cd aiml
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8001
```

> The service uses `text-embedding-3-small` and requires `OPENAI_API_KEY`.

## API

- `GET /health`
- `POST /relations`
  - Input: chunks + threshold
  - Output: cross-note semantic edges with cosine similarity scores.
- `POST /embeddings`
  - Input: list of texts
  - Output: normalized embedding vectors for pgvector storage.
