# user-progress

Spring Boot service for authentication and watch progress.

## Run

    ./mvnw spring-boot:run

Service listens on http://localhost:8081

- Health: http://localhost:8081/user-progress/health
- Swagger UI: http://localhost:8081/swagger-ui.html
- Actuator health: http://localhost:8081/actuator/health

## Test

    ./mvnw verify

Regenerate stubs from the OpenAPI spec before building:

    ../../api/scripts/gen-all.sh
