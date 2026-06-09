# chat

Spring Boot service orchestrating spoiler-safe Q&A: reads the user's watch progress,
fetches allowed episode summaries from the shared catalog tables, calls the GenAI
service, and persists Q&A history.

## Run (from `services/` parent)

```bash
./mvnw -pl chat spring-boot:run
```

Requires Postgres with catalog + user-progress schemas populated (use `docker compose`
for the full stack). Set `GENAI_BASE_URL` if genai is not on `http://localhost:8084`.

Listens on http://localhost:8083

- Health: http://localhost:8083/chat/health
- Ask: `POST /chat/questions` (JWT required)
- Swagger UI: http://localhost:8083/swagger-ui.html
- Actuator health: http://localhost:8083/actuator/health

## Test (from `services/` parent)

```bash
./mvnw -pl chat verify
```

Integration tests use Testcontainers Postgres and a mocked GenAI backend.

## Regenerate API stubs (from repo root)

```bash
./api/scripts/gen-all.sh java
```

## Manual test

```bash
# After docker compose up and logging in at :8080, copy your JWT:
TOKEN="..."
SERIES_ID="958661e6-226c-5117-9318-5e3265598767"  # Stranger Things

curl -s http://localhost:8080/chat/questions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"seriesId\":\"$SERIES_ID\",\"question\":\"Who is Eleven?\"}" | jq
```

### Spoiler trap test cases

| Progress | Question | Expected |
|----------|----------|----------|
| S1E1 (index 1) | "Who is Eleven?" | Answer from E1 only; citations ≤ 1 |
| S1E3 (index 3) | "What happens to Barb?" | May cite E2–E3; must not cite E4+ |
| S1E5 (index 5) | "Who is Billy?" (S2 character) | Refuse or say context insufficient; no S2 spoilers |
| 0 (unset) | any question | 400 `NO_PROGRESS` |
