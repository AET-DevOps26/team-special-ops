# catalog

Spring Boot service for shows, episodes, and per-episode summaries.

## Run

    ./mvnw spring-boot:run

Service listens on http://localhost:8082

- Health: http://localhost:8082/catalog/health
- Swagger UI: http://localhost:8082/swagger-ui.html
- Actuator health: http://localhost:8082/actuator/health

## Test

    ./mvnw verify

Regenerate stubs from the OpenAPI spec before building:

    ../../api/scripts/gen-all.sh
