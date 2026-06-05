from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator
from pydantic import BaseModel

app = FastAPI(title="GenAI Service", version="0.1.0")


class HealthStatus(BaseModel):
    status: str
    service: str


@app.get("/genai/health", response_model=HealthStatus, tags=["genai"])
async def health() -> HealthStatus:
    return HealthStatus(status="ok", service="genai")


Instrumentator().instrument(app).expose(app, endpoint="/metrics")
