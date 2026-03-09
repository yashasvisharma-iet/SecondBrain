from __future__ import annotations

import os
import re
import hashlib
from typing import List
from dotenv import load_dotenv
load_dotenv()
import numpy as np
from fastapi import FastAPI
from openai import OpenAI
from pydantic import BaseModel, Field

app = FastAPI(title="SecondBrain AIML", version="0.2.0")

MODEL_NAME = "text-embedding-3-small"
FALLBACK_MODEL_NAME = "hashed-bow-v1"
FALLBACK_DIMENSIONS = 512
openai_client = OpenAI(api_key=os.getenv("OPENAI_API_KEY")) if os.getenv("OPENAI_API_KEY") else None


class ChunkInput(BaseModel):
    id: str = Field(..., description="Unique chunk id, e.g. c-<chunkId>")
    raw_note_id: str = Field(..., description="Parent note id")
    content: str = Field(..., description="Chunk text")


class SimilarityRequest(BaseModel):
    chunks: List[ChunkInput] = Field(default_factory=list)
    threshold: float = Field(default=0.8, ge=0.0, le=1.0)


class SimilarityEdge(BaseModel):
    source: str
    target: str
    score: float


class SimilarityResponse(BaseModel):
    edges: List[SimilarityEdge]


class EmbeddingRequest(BaseModel):
    texts: List[str] = Field(default_factory=list)


class EmbeddingResponse(BaseModel):
    embeddings: List[List[float]]


class SummaryRequest(BaseModel):
    text: str = Field(default="")


class SummaryResponse(BaseModel):
    summary: str


def _normalize_embeddings(vectors: list[list[float]]) -> np.ndarray:
    array = np.asarray(vectors, dtype=float)
    norms = np.linalg.norm(array, axis=1, keepdims=True)
    norms[norms == 0] = 1.0
    return array / norms


def _embed_texts(texts: list[str]) -> np.ndarray:
    if not os.getenv("OPENAI_API_KEY"):
        return _fallback_embed_texts(texts)

    response = openai_client.embeddings.create(model=MODEL_NAME, input=texts)
    vectors = [item.embedding for item in response.data]
    return _normalize_embeddings(vectors)


def _fallback_embed_texts(texts: list[str]) -> np.ndarray:
    vectors: list[list[float]] = []
    for text in texts:
        vec = np.zeros(FALLBACK_DIMENSIONS, dtype=float)
        tokens = re.findall(r"[A-Za-z0-9]{3,}", (text or "").lower())
        for token in tokens:
            digest = hashlib.blake2b(token.encode("utf-8"), digest_size=8).digest()
            bucket = int.from_bytes(digest, "big") % FALLBACK_DIMENSIONS
            vec[bucket] += 1.0
        vectors.append(vec.tolist())
    return _normalize_embeddings(vectors)


def _summarize_text(text: str) -> str:
    cleaned = (text or "").strip()
    if not cleaned:
        return "I need note content before I can summarize it."

    if openai_client:
        completion = openai_client.chat.completions.create(
            model="gpt-4.1-mini",
            messages=[
                {"role": "system", "content": "Summarize user notes in 4-6 concise bullet points. Keep key actions and facts."},
                {"role": "user", "content": cleaned},
            ],
            temperature=0.2,
        )
        content = completion.choices[0].message.content if completion.choices else ""
        return (content or "").strip() or "I couldn't generate a summary right now."

    sentences = re.split(r"(?<=[.!?])\s+", cleaned)
    preview = " ".join(sentences[:3]).strip()
    return preview if preview else cleaned[:360]


@app.get("/health")
def health() -> dict[str, str]:
    using_openai = bool(os.getenv("OPENAI_API_KEY"))
    return {
        "status": "ok",
        "model": MODEL_NAME if using_openai else FALLBACK_MODEL_NAME,
        "provider": "openai" if using_openai else "local-fallback",
    }


@app.post("/relations", response_model=SimilarityResponse)
def relations(payload: SimilarityRequest) -> SimilarityResponse:
    chunks = [chunk for chunk in payload.chunks if chunk.content and chunk.content.strip()]
    # if len(chunks) < 2:
    #     return SimilarityResponse(edges=[])

    #embeds all chunks 
    corpus = [chunk.content for chunk in chunks]
    embeddings = _embed_texts(corpus)
    similarities = np.matmul(embeddings, embeddings.T)

    edges: list[SimilarityEdge] = []
    n = len(chunks)
    for i in range(n):
        for j in range(i + 1, n):
            left = chunks[i]
            right = chunks[j]

            # semantic relations should connect different notes only
            if left.raw_note_id == right.raw_note_id:
                continue

            score = float(np.clip(similarities[i, j], 0.0, 1.0))
            if score >= payload.threshold:
                edges.append(
                    SimilarityEdge(
                        source=left.id,
                        target=right.id,
                        score=round(score, 4),
                    )
                )

    return SimilarityResponse(edges=edges)


@app.post("/embeddings", response_model=EmbeddingResponse)
def embeddings(payload: EmbeddingRequest) -> EmbeddingResponse:
    texts = [text if text is not None else "" for text in payload.texts]
    if not texts:
        return EmbeddingResponse(embeddings=[])

    vectors = _embed_texts(texts)
    return EmbeddingResponse(embeddings=vectors.tolist())


@app.post("/summarize", response_model=SummaryResponse)
def summarize(payload: SummaryRequest) -> SummaryResponse:
    return SummaryResponse(summary=_summarize_text(payload.text))
