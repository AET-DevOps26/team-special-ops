from fastapi.testclient import TestClient

from genai.main import app


def test_metrics_endpoint_exposes_prometheus_text():
    client = TestClient(app)

    # Generate some request metrics first.
    health_response = client.get("/genai/health")
    assert health_response.status_code == 200

    response = client.get("/metrics")
    assert response.status_code == 200
    assert "text/plain" in response.headers["content-type"]

    body = response.text
    assert "# HELP" in body
    assert "# TYPE" in body
    assert "http_request" in body
