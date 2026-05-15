# catalog

Spring Boot service for shows, episodes, and per-episode summaries.

## Endpoints

- `GET /catalog/health` — liveness probe
- `GET /catalog/shows` — list all shows
- `GET /catalog/shows/{id}/episodes` — episodes for a show, ordered by global `episodeIndex`

Full schema: see `api/openapi.yaml` or http://localhost:8082/swagger-ui.html when running.

## Schema

Two tables, owned by this service, in the shared Postgres `public` schema:

- `show` — `id (UUID)`, `title`, `seasons_count`, `episodes_count`, `created_at`
- `episode` — `id (UUID)`, `show_id`, `season`, `episode_number`, `episode_index`, `title`, `summary`, `created_at`

`episode_index` is the global 1..N index across the whole show, used by the chat service's spoiler-safe filter (`episode_index <= progress`).

Schema is managed by [Flyway](https://flywaydb.org/). Migration files live in `src/main/resources/db/migration/` and run automatically on application startup.

## Seeding a new show

Episode data is sourced from Wikipedia (per-episode "Plot" sections, or "ShortSummary" template fields on the per-season episode tables) by a dev-time Python script that produces a static SQL migration. The script is **not** invoked at runtime.

```bash
uv run --project services/catalog/scripts services/catalog/scripts/fetch_show_seed.py \
    "Stranger Things" \
    --episode-list-page "List of Stranger Things episodes" \
    --slug stranger_things \
    --output services/catalog/src/main/resources/db/migration/V2__seed_stranger_things.sql
```

Commit the generated `.sql` file like any other migration. Next run of the service applies it.

### Attribution

Episode summaries are derived from English Wikipedia and licensed under [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/). Source page references are included as comments at the end of each `V*__seed_*.sql` migration.

## Run (from `services/` parent)

    ./mvnw -pl catalog spring-boot:run

Requires a reachable Postgres at `localhost:5432` (db/user/password `tso`) unless overridden via:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Listens on http://localhost:8082.

- Health: http://localhost:8082/catalog/health
- Swagger UI: http://localhost:8082/swagger-ui.html
- Actuator health: http://localhost:8082/actuator/health

## Test (from `services/` parent)

    ./mvnw -pl catalog verify

Integration tests use [Testcontainers](https://www.testcontainers.org/) to spin up a real Postgres container. Docker must be running.

## Regenerate API stubs (from repo root)

    ./api/scripts/gen-all.sh java
