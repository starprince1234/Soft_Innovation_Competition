from fastapi import FastAPI

app = FastAPI(title="Mock VLM")


@app.get("/v1/models")
async def models():
    return {
        "object": "list",
        "data": [
            {
                "id": "qwen3-vl-lora",
                "object": "model",
                "created": 0,
                "owned_by": "mock",
            }
        ],
    }


@app.post("/v1/chat/completions")
async def chat_completions(_payload: dict):
    # Return OpenAI-compatible response payload.
    return {
        "id": "chatcmpl-mock-001",
        "object": "chat.completion",
        "created": 0,
        "model": "qwen3-vl-lora",
        "choices": [
            {
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": '{"name":"青铜神树","confidence":0.93,"category":"青铜器","era":"商代","description":"古蜀文明代表性青铜祭祀器"}',
                },
                "finish_reason": "stop",
            }
        ],
        "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2},
    }
