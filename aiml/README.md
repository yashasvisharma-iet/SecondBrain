# AIML service (FastAPI)

Computes semantic relationships between text chunks using sentence-transformer embeddings in Python.

## Run

```bash
cd aiml
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8001
```

> The service uses `sentence-transformers/all-MiniLM-L6-v2` and downloads model weights on first run.

## API

- `GET /health`
- `POST /relations`
  - Input: chunks + threshold
  - Output: cross-note semantic edges with cosine similarity scores.
- `POST /embeddings`
  - Input: list of texts
  - Output: normalized embedding vectors (MiniLM 384-dim) for pgvector storage.
