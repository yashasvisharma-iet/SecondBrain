from __future__ import annotations

import os
from typing import List

import numpy as np
from fastapi import FastAPI, HTTPException
from openai import OpenAI
from pydantic import BaseModel, Field

app = FastAPI(title="SecondBrain AIML", version="0.2.0")

MODEL_NAME = "text-embedding-3-small"
openai_client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))


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


def _normalize_embeddings(vectors: list[list[float]]) -> np.ndarray:
    array = np.asarray(vectors, dtype=float)
    norms = np.linalg.norm(array, axis=1, keepdims=True)
    norms[norms == 0] = 1.0
    return array / norms


def _embed_texts(texts: list[str]) -> np.ndarray:
    if not os.getenv("OPENAI_API_KEY"):
        raise HTTPException(status_code=500, detail="OPENAI_API_KEY is required")

    response = openai_client.embeddings.create(model=MODEL_NAME, input=texts)
    vectors = [item.embedding for item in response.data]
    return _normalize_embeddings(vectors)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "model": MODEL_NAME, "provider": "openai"}


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
