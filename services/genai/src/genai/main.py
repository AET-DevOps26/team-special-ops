from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator
from pydantic import BaseModel

from genai.routes.ask import router as ask_router

app = FastAPI(title="GenAI Service", version="0.1.0")
app.include_router(ask_router)


class HealthStatus(BaseModel):
    status: str
    service: str


@app.get("/genai/health", response_model=HealthStatus, tags=["genai"])
async def health() -> HealthStatus:
    return HealthStatus(status="ok", service="genai")


Instrumentator().instrument(app).expose(app, endpoint="/metrics")
