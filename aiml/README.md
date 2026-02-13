# AIML service (FastAPI)

Computes semantic relationships between text chunks using embeddings in Python.

## Run

```bash
cd aiml
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8001
```

## API

- `GET /health`
- `POST /relations`
  - Input: chunks + threshold
  - Output: cross-note semantic edges with cosine similarity scores.
