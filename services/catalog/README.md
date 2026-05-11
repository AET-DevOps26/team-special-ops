# catalog

Spring Boot service for shows, episodes, and per-episode summaries.

## Run (from `services/` parent)

    ./mvnw -pl catalog spring-boot:run

Listens on http://localhost:8082

- Health: http://localhost:8082/catalog/health
- Swagger UI: http://localhost:8082/swagger-ui.html
- Actuator health: http://localhost:8082/actuator/health

## Test (from `services/` parent)

    ./mvnw -pl catalog verify

## Regenerate API stubs (from repo root)

    ./api/scripts/gen-all.sh java
