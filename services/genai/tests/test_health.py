from fastapi.testclient import TestClient

from genai.main import app


def test_health_returns_ok_and_service_name():
    client = TestClient(app)
    response = client.get("/genai/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "genai"}
