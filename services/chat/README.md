# chat

Spring Boot service orchestrating Q&A: receives a question, fetches allowed
episode summaries, calls the GenAI service, persists Q&A history.

## Run (from `services/` parent)

    ./mvnw -pl chat spring-boot:run

Listens on http://localhost:8083

- Health: http://localhost:8083/chat/health
- Swagger UI: http://localhost:8083/swagger-ui.html
- Actuator health: http://localhost:8083/actuator/health

## Test (from `services/` parent)

    ./mvnw -pl chat verify

## Regenerate API stubs (from repo root)

    ./api/scripts/gen-all.sh java
