# user-progress

Spring Boot service for authentication and watch progress.

## Run (from `services/` parent)

    ./mvnw -pl user-progress spring-boot:run

Listens on http://localhost:8081

- Health: http://localhost:8081/user-progress/health
- Swagger UI: http://localhost:8081/swagger-ui.html
- Actuator health: http://localhost:8081/actuator/health

## Test (from `services/` parent)

    ./mvnw -pl user-progress verify

## Regenerate API stubs (from repo root)

    ./api/scripts/gen-all.sh java
