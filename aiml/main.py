from __future__ import annotations

from typing import List

import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

app = FastAPI(title="SecondBrain AIML", version="0.2.0")

# Small sentence-transformer provides semantic embeddings while keeping inference lightweight.
MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"
embedding_model = SentenceTransformer(MODEL_NAME)


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


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "model": MODEL_NAME}


@app.post("/relations", response_model=SimilarityResponse)
def relations(payload: SimilarityRequest) -> SimilarityResponse:
    chunks = [chunk for chunk in payload.chunks if chunk.content and chunk.content.strip()]
    if len(chunks) < 2:
        return SimilarityResponse(edges=[])

    corpus = [chunk.content for chunk in chunks]
    embeddings = embedding_model.encode(corpus, normalize_embeddings=True)
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
