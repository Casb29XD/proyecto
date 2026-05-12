import logging
import os
from typing import Dict, List

import numpy as np
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from gensim import downloader as api
from gensim.models import KeyedVectors
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

logger = logging.getLogger("ai-engine")
logging.basicConfig(level=logging.INFO)

app = FastAPI(title="AI Engine", version="1.0.0")


class SimilarityRequest(BaseModel):
    source: str = Field(..., min_length=1)
    target: str = Field(..., min_length=1)


class SimilarityResponse(BaseModel):
    similarity: float
    model: str


class ErrorEnvelope(BaseModel):
    error: Dict[str, str]


WORD2VEC_MODEL_NAME = os.getenv("WORD2VEC_MODEL_NAME", "glove-wiki-gigaword-50")
SBERT_MODEL_NAME = os.getenv("SBERT_MODEL_NAME", "paraphrase-multilingual-MiniLM-L12-v2")

word_vectors: KeyedVectors | None = None
sbert_model: SentenceTransformer | None = None


@app.on_event("startup")
def startup() -> None:
    global word_vectors, sbert_model
    logger.info("Cargando modelo KeyedVectors: %s", WORD2VEC_MODEL_NAME)
    word_vectors = api.load(WORD2VEC_MODEL_NAME)
    logger.info("Cargando modelo SentenceTransformer: %s", SBERT_MODEL_NAME)
    sbert_model = SentenceTransformer(SBERT_MODEL_NAME)
    logger.info("Modelos cargados correctamente.")


@app.exception_handler(Exception)
async def unhandled_exception_handler(_: Request, exc: Exception):
    logger.exception("Error interno en ai-engine: %s", exc)
    return JSONResponse(
        status_code=500,
        content=ErrorEnvelope(error={"code": "INTERNAL_ERROR", "message": "Error interno del motor de IA"}).model_dump(),
    )


@app.get("/health")
def health() -> Dict[str, str]:
    if word_vectors is None or sbert_model is None:
        return {"status": "degraded"}
    return {"status": "ok"}


@app.post("/v1/similarity/word2vec", response_model=SimilarityResponse, responses={500: {"model": ErrorEnvelope}})
def similarity_word2vec(payload: SimilarityRequest) -> SimilarityResponse:
    if word_vectors is None:
        raise RuntimeError("Modelo Word2Vec no disponible")

    source_vec = sentence_vector(payload.source, word_vectors)
    target_vec = sentence_vector(payload.target, word_vectors)
    score = cosine_similarity(source_vec, target_vec)

    return SimilarityResponse(similarity=normalize_score(score), model=f"keyedvectors:{WORD2VEC_MODEL_NAME}")


@app.post("/v1/similarity/sbert", response_model=SimilarityResponse, responses={500: {"model": ErrorEnvelope}})
def similarity_sbert(payload: SimilarityRequest) -> SimilarityResponse:
    if sbert_model is None:
        raise RuntimeError("Modelo SBERT no disponible")

    embeddings = sbert_model.encode([payload.source, payload.target], normalize_embeddings=True)
    score = float(np.dot(embeddings[0], embeddings[1]))
    return SimilarityResponse(similarity=normalize_score(score), model=f"sbert:{SBERT_MODEL_NAME}")


def sentence_vector(text: str, vectors: KeyedVectors) -> np.ndarray:
    tokens = tokenize(text)
    known_vectors: List[np.ndarray] = [vectors[token] for token in tokens if token in vectors]
    if not known_vectors:
        return np.zeros(vectors.vector_size, dtype=np.float32)
    return np.mean(known_vectors, axis=0)


def tokenize(text: str) -> List[str]:
    return [token.strip().lower() for token in text.split() if token.strip()]


def cosine_similarity(source: np.ndarray, target: np.ndarray) -> float:
    source_norm = np.linalg.norm(source)
    target_norm = np.linalg.norm(target)
    if source_norm == 0.0 or target_norm == 0.0:
        return 0.0
    return float(np.dot(source, target) / (source_norm * target_norm))


def normalize_score(score: float) -> float:
    if np.isnan(score) or np.isinf(score):
        return 0.0
    return float(max(0.0, min(1.0, score)))
