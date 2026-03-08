from __future__ import annotations

import json
import os
from typing import Any, TypedDict
from urllib import error, request as urllib_request

from dotenv import load_dotenv
from flask import Flask, jsonify, request
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from langgraph.graph import END, START, StateGraph

load_dotenv()


class BotState(TypedDict):
    user_message: str
    retrieval_answer: str
    retrieval_context: str
    answer: str


SYSTEM_PROMPT = (
    "You are SecondBrain AI assistant. Help the user understand their notes, "
    "suggest connections, and provide concise actionable next steps."
)

BACKEND_ASK_URL = os.getenv("BACKEND_ASK_URL", "http://localhost:8080/api/graph/ask")
CHAT_REQUIRE_RETRIEVAL = os.getenv("CHAT_REQUIRE_RETRIEVAL", "true").lower() in {"1", "true", "yes", "on"}


def _fetch_retrieval_context(message: str) -> dict[str, Any]:
    payload = json.dumps({"query": message}).encode("utf-8")
    req = urllib_request.Request(
        BACKEND_ASK_URL,
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib_request.urlopen(req, timeout=8) as response:
            body = response.read().decode("utf-8")
            data = json.loads(body)
            citations = data.get("citations", []) if isinstance(data, dict) else []
            answer = data.get("answer", "") if isinstance(data, dict) else ""
            return {"answer": answer, "citations": citations}
    except (error.URLError, TimeoutError, json.JSONDecodeError, ValueError) as exc:
        return {"answer": "", "citations": [], "error": str(exc)}

def _citation_only_answer(message: str, citations: list[dict[str, Any]]) -> str:
    if not citations:
        return ""

    top = citations[0]
    snippet = str(top.get("snippet", "")).strip()
    page_id = str(top.get("pageId", "unknown-page"))
    count = len(citations)

    if snippet:
        return (
            f'I found {count} relevant chunk(s) for "{message}". '
            f'Best match from {page_id}: {snippet}'
        )

    return f'I found {count} relevant chunk(s) for "{message}".'


def _chat_response(answer: str, citations: list[dict[str, Any]], source: str, backend_error: str = ""):
    payload: dict[str, Any] = {"answer": answer, "citations": citations, "source": source}
    if backend_error:
        payload["backend_error"] = backend_error
    return jsonify(payload)


def _build_graph(model: ChatOpenAI):
    graph = StateGraph(BotState)

    def generate(state: BotState) -> BotState:
        response = model.invoke(
            [
                SystemMessage(content=SYSTEM_PROMPT),
                HumanMessage(
                    content=(
                        f"User question: {state['user_message']}\n\n"
                        f"Retrieved service answer:\n{state['retrieval_answer'] or 'None'}\n\n"
                        f"Retrieved knowledge base context:\n{state['retrieval_context']}\n\n"
                        "Rewrite the backend-retrieved answer in a concise, helpful way."
                    )
                ),
            ]
        )

        if isinstance(response, AIMessage):
            answer = response.content
        else:
            answer = str(response)

        return {"answer": answer}

    graph.add_node("generate", generate)
    graph.add_edge(START, "generate")
    graph.add_edge("generate", END)
    return graph.compile()


app = Flask(__name__)

api_key = os.getenv("OPENAI_API_KEY")
chat_model = ChatOpenAI(model=os.getenv("OPENAI_CHAT_MODEL", "gpt-4o-mini"), api_key=api_key, temperature=0.2) if api_key else None
chat_graph = _build_graph(chat_model) if chat_model else None


@app.get("/health")
def health():
    return jsonify(
        {
            "status": "ok",
            "provider": "openai" if chat_model else "missing-openai-key",
            "graph": "langgraph",
            "backend_ask_url": BACKEND_ASK_URL,
            "chat_require_retrieval": CHAT_REQUIRE_RETRIEVAL,
        }
    )


@app.post("/chat")
def chat():
    if not chat_graph:
        return jsonify({"error": "OPENAI_API_KEY is not configured"}), 500

    payload = request.get_json(silent=True) or {}
    message = str(payload.get("message", "")).strip()

    if not message:
        return jsonify({"error": "message is required"}), 400

    retrieval = _fetch_retrieval_context(message)
    citations = retrieval.get("citations", [])
    retrieval_answer = str(retrieval.get("answer", "")).strip()
    retrieval_error = str(retrieval.get("error", "")).strip()

    if retrieval_answer:
        return _chat_response(retrieval_answer, citations, source="retrieval_answer")

    if citations:
        return _chat_response(_citation_only_answer(message, citations), citations, source="retrieval_citations")

    if CHAT_REQUIRE_RETRIEVAL and not citations:
        return jsonify(
            {
                "error": (
                    "Retrieval backend did not return a result. "
                    "Set BACKEND_ASK_URL to a reachable /api/graph/ask endpoint."
                ),
                "backend_ask_url": BACKEND_ASK_URL,
                "backend_error": retrieval_error or "unknown",
            }
        ), 502

    if citations:
        return jsonify({"answer": _citation_only_answer(message, citations), "citations": citations})

    if CHAT_REQUIRE_RETRIEVAL and not citations:
        return jsonify(
            {
                "error": (
                    "Retrieval backend did not return a result. "
                    "Set BACKEND_ASK_URL to a reachable /api/graph/ask endpoint."
                ),
                "backend_ask_url": BACKEND_ASK_URL,
                "backend_error": retrieval_error or "unknown",
            }
        ), 502

    context_lines = []
    for citation in citations[:5]:
        page_id = citation.get("pageId", "unknown-page")
        chunk_index = citation.get("chunkIndex", "?")
        snippet = citation.get("snippet", "")
        context_lines.append(f"- {page_id} | chunk {chunk_index}: {snippet}")

    retrieval_context = "\n".join(context_lines) if context_lines else "No retrieved context available."

    result = chat_graph.invoke(
        {
            "user_message": message,
            "retrieval_answer": retrieval_answer,
            "retrieval_context": retrieval_context,
            "answer": "",
        }
    )
    answer = str(result.get("answer", "")).strip()
    return _chat_response(answer, citations, source="llm_fallback", backend_error=retrieval_error)


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8002, debug=False)
