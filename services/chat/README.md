# chat

Spring Boot service orchestrating Q&A: receives a question, fetches allowed
episode summaries, calls the GenAI service, persists Q&A history.

## Run

    ./mvnw spring-boot:run

Service listens on http://localhost:8083

- Health: http://localhost:8083/chat/health
- Swagger UI: http://localhost:8083/swagger-ui.html
- Actuator health: http://localhost:8083/actuator/health

## Test

    ./mvnw verify

Regenerate stubs from the OpenAPI spec before building:

    ../../api/scripts/gen-all.sh
