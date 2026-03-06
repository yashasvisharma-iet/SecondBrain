from __future__ import annotations

import os
from typing import TypedDict

from dotenv import load_dotenv
from flask import Flask, jsonify, request
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from langgraph.graph import END, START, StateGraph

load_dotenv()


class BotState(TypedDict):
    user_message: str
    answer: str


SYSTEM_PROMPT = (
    "You are SecondBrain AI assistant. Help the user understand their notes, "
    "suggest connections, and provide concise actionable next steps."
)


def _build_graph(model: ChatOpenAI):
    graph = StateGraph(BotState)

    def generate(state: BotState) -> BotState:
        response = model.invoke(
            [
                SystemMessage(content=SYSTEM_PROMPT),
                HumanMessage(content=state["user_message"]),
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

    result = chat_graph.invoke({"user_message": message, "answer": ""})
    answer = result.get("answer", "")

    return jsonify({"answer": answer})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8002, debug=False)
