# Catalog Service — Schema, Seed, and List Endpoints (Backlog Item #4)

**Status:** Design (awaiting review)
**Date:** 2026-05-15
**Scope:** Full Backlog Item #4 from `docs/system-architecture.md` — Postgres schema for `Show` and `Episode`, seed data for a chosen show, and the two list endpoints. OpenAPI, tests, and README are part of the same task per project DoD.
**Show chosen for first seed:** *Stranger Things* (42 episodes across 4 seasons at time of writing).

---

## 1. Goals

1. Define the Postgres schema for `show` and `episode` owned by the catalog service.
2. Ship a reproducible seed pipeline that turns a Wikipedia show into a committed SQL migration.
3. Expose `GET /catalog/shows` and `GET /catalog/shows/{id}/episodes` over REST, conforming to `api/openapi.yaml`.
4. Cover the above with tests that exercise real Postgres (Testcontainers), not an in-memory dialect.

## 2. Non-goals

- A `Character` table or any first-class entity extraction. Deferred per the "LLM-assisted ingestion" line in the post-MVP list — adding characters now would force a first-appearance-episode design for spoiler safety, which is its own task.
- Runtime fetching from Wikipedia or any third party. The catalog service must boot cleanly with zero external dependencies.
- Cross-service joins. The chat service will eventually call the catalog REST endpoints, not query the catalog tables directly.
- Multiple shows in this PR. The schema and endpoints support N shows; the seed loads one.

## 3. Schema

Plain Postgres DDL, applied via Flyway. Lives in `services/catalog/src/main/resources/db/migration/V1__create_catalog_schema.sql`:

```sql
CREATE TABLE show (
  id               UUID         PRIMARY KEY,
  title            TEXT         NOT NULL UNIQUE,
  seasons_count    INT          NOT NULL,
  episodes_count   INT          NOT NULL,
  created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE episode (
  id              UUID         PRIMARY KEY,
  show_id         UUID         NOT NULL REFERENCES show(id) ON DELETE CASCADE,
  season          INT          NOT NULL,
  episode_number  INT          NOT NULL,
  episode_index   INT          NOT NULL,
  title           TEXT         NOT NULL,
  summary         TEXT         NOT NULL,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

  UNIQUE (show_id, season, episode_number),
  UNIQUE (show_id, episode_index)
);

CREATE INDEX idx_episode_show_index ON episode(show_id, episode_index);
```

Design notes:

- **`episode_index`** is the load-bearing column for spoiler safety. `system-architecture.md` describes the chat-service safety filter as a single comparison `episode_index ≤ progress`. The `(show_id, episode_index)` index makes that filter O(log n) and translates cleanly to vector-DB metadata filtering when RAG is added later.
- **Two unique constraints** prevent both kinds of duplicates: re-running the seed twice, and a script bug that miscomputes the global index.
- **`seasons_count` / `episodes_count`** are denormalized on `show` because they appear on the class diagram and because the `GET /catalog/shows` response uses them — keeping them on the row avoids a `GROUP BY` on every catalog list call. Cost is one consistency rule the seed script must enforce.
- **`ON DELETE CASCADE`** — episodes are meaningless without their show.
- **UUID primary keys** — matches the class diagram and avoids ID collisions when the chat service starts referencing episodes in `cites` rows.

## 4. Schema management — Flyway

- Add `org.flywaydb:flyway-core` and the Postgres-specific module to `services/catalog/pom.xml`.
- Migrations live in `services/catalog/src/main/resources/db/migration/`.
- File naming: `V<n>__<snake_case_description>.sql`. Flyway runs each file once per database, tracks state in `flyway_schema_history`, and errors on checksum mismatch (so applied files cannot be silently edited).
- `application.yml` config: `spring.flyway.enabled: true` (default). No explicit `schemas:` — we use the default `public` schema for v0 since all three services share one database. (Schema-per-service is a later optimization.)
- Spring Boot auto-runs Flyway on startup against the configured `DataSource`.

The other two Spring services will adopt the same pattern when they add their own tables. This task is the place to make the choice.

## 5. Seed pipeline

Two artifacts:

1. **`services/catalog/scripts/fetch_show_seed.py`** — dev-time script, never shipped to runtime.
2. **`services/catalog/src/main/resources/db/migration/V2__seed_stranger_things.sql`** — the static SQL the script produces, committed to git.

### 5.1 Script

```text
Usage:
  uv run fetch_show_seed.py "Stranger Things" \
      --output ../src/main/resources/db/migration/V2__seed_stranger_things.sql
```

Pipeline:

1. Fetch the show's "List of <Show> episodes" article via the MediaWiki Action API (`action=parse&prop=wikitext`).
2. Parse the per-season episode tables: `(season, episode_number, title, episode_article_title)`.
3. For each episode, fetch its article and extract the **"Plot"** section text via `action=parse&prop=text&section=Plot`. Fall back to "Synopsis" or the article lede when "Plot" is absent.
4. Strip HTML, citation markers (`[1]`, `[citation needed]`), and decode HTML entities.
5. Compute global `episode_index = 1..N` in airing order across seasons.
6. Compute `seasons_count = max(season)` and `episodes_count = count(*)`.
7. Generate deterministic UUIDs: `show.id = uuid5(NAMESPACE_DNS, "show:stranger_things")` and `episode.id = uuid5(NAMESPACE_DNS, "episode:stranger_things:s<season>e<episode_number>")`. This keeps the SQL byte-for-byte reproducible across script re-runs, so `git diff` only changes when the data actually changes.
8. Emit a single SQL file:
   - One `INSERT INTO show` row.
   - 42 `INSERT INTO episode` rows.
   - Trailing comment block: source article URLs, CC BY-SA 3.0 attribution, script version, run timestamp.
9. Use dollar-quoted string literals (`$$...$$`) for the `summary` text to sidestep apostrophe escaping in show prose.

The script lives in Python because the GenAI service already uses Python + `uv`, so the toolchain is available. A minimal `pyproject.toml` under `services/catalog/scripts/` declares `requests` + `beautifulsoup4`.

### 5.2 Why fetch-once-and-commit, not fetch-at-runtime

- CI runs are deterministic and offline.
- PR reviewers see the actual seed data in the diff.
- Wikipedia outage cannot break `docker compose up`.
- The committed SQL is the source of truth; the script is just how we produced it.

### 5.3 Edge cases handled by the script

- Double-length premieres/finales (one Wikipedia row → one episode row; no special-casing needed).
- "Plot" section sometimes lives under a different heading — fallback chain: "Plot" → "Synopsis" → article lede.
- Citation markers and HTML entities stripped before SQL emission.
- Single quotes in summary text — handled by dollar-quoting.

## 6. Java layer

### 6.1 Entities

Located at `services/catalog/src/main/java/com/tso/catalog/domain/`.

```java
@Entity @Table(name = "show")
class Show {
  @Id UUID id;
  String title;
  int seasonsCount;
  int episodesCount;
  // getters; no setters in MVP (catalog is read-only at runtime).
}

@Entity @Table(name = "episode")
class Episode {
  @Id UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "show_id", nullable = false)
  Show show;

  int season;
  int episodeNumber;
  int episodeIndex;
  String title;
  String summary;
}
```

Notes:

- `@ManyToOne(fetch = LAZY)` — the relationship is real in the domain. Lazy avoids paying for the show when it isn't needed.
- N+1 is the obvious risk. Mitigation: when a repository method needs the show eagerly, declare it via `@EntityGraph(attributePaths = "show")` or a `JOIN FETCH` JPQL query. The two endpoints in this task don't need it, so the default repository methods stay lazy.
- JSON serialization recursion is prevented by always mapping entities → DTOs in the controller (see §6.3). Jackson never sees the entity graph.

### 6.2 Repositories

`services/catalog/src/main/java/com/tso/catalog/repo/`:

```java
interface ShowRepository extends Repository<Show, UUID> {
  List<Show> findAllByOrderByTitleAsc();
  Optional<Show> findById(UUID id);
}

interface EpisodeRepository extends Repository<Episode, UUID> {
  List<Episode> findByShow_IdOrderByEpisodeIndexAsc(UUID showId);
}
```

Spring Data generates the implementations from the method names.

### 6.3 Controllers

The OpenAPI generator already produces a `CatalogApi` interface. We implement it the same way `HealthController` does today — one `@RestController` class per concern.

- `GET /catalog/shows` → 200 with `ShowSummaryDto[]` ordered by title.
- `GET /catalog/shows/{id}/episodes` → 200 with `EpisodeDto[]` ordered by `episodeIndex`; 404 with `Error` body when the show id does not exist.

Two-layer DTO mapping (entities → DTOs) lives in the controller. DTOs are nearly 1:1 with entities today but decouple the wire schema from the JPA model.

## 7. OpenAPI changes

Add to `api/openapi.yaml`:

- `GET /catalog/shows`
  - Tags: `[catalog]`
  - 200: `application/json` → `ShowSummary[]`
- `GET /catalog/shows/{id}/episodes`
  - Tags: `[catalog]`
  - Path param `id` (UUID).
  - 200: `application/json` → `Episode[]`
  - 404: `application/json` → `Error`

New schemas:

- `ShowSummary`: `{ id: UUID, title: string, seasonsCount: int, episodesCount: int }`
- `Episode`: `{ id: UUID, season: int, episodeNumber: int, episodeIndex: int, title: string, summary: string }`

`Error` reused as-is.

Run `./api/scripts/gen-all.sh` after editing `openapi.yaml`. The regenerated `CatalogApi` interface drives the controller's required method signatures.

## 8. Tests

Test class: `ShowControllerTest` under `services/catalog/src/test/java/com/tso/catalog/`.

- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@Testcontainers` + `PostgreSQLContainer`.
- Boots the app against a real Postgres container, with Flyway running both `V1` and `V2` migrations.
- Cases:
  1. `GET /catalog/shows` returns exactly one entry whose title is `"Stranger Things"` with the seeded season/episode counts.
  2. `GET /catalog/shows/{id}/episodes` for that show returns 42 entries, ordered by `episodeIndex`, each with a non-empty `summary`, `season`, `episodeNumber`, and `title`.
  3. `GET /catalog/shows/{random-uuid}/episodes` returns 404 with the `Error` shape.

`HealthControllerTest` is left untouched.

**Why Testcontainers, not H2:** the migrations are real Postgres SQL. H2's compatibility modes silently rewrite or reject Postgres-specific syntax (`UUID`, `TIMESTAMPTZ`, `$$`-quoted strings), so green H2 tests can mask broken production migrations. Testcontainers makes the test environment match production.

CI cost: one Postgres container per test class, a few seconds. Acceptable for a project that already runs Docker in CI for multi-stage image builds.

## 9. Configuration

`services/catalog/src/main/resources/application.yml`:

```yaml
server:
  port: 8082

spring:
  application:
    name: catalog
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://postgres:5432/tso}
    username: ${SPRING_DATASOURCE_USERNAME:tso}
    password: ${SPRING_DATASOURCE_PASSWORD:tso}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc.time_zone: UTC
  flyway:
    enabled: true
```

`ddl-auto: validate` is deliberate — Hibernate verifies that the JPA mapping matches the DB schema at startup but never writes DDL. Flyway is the only thing that creates tables.

Local non-Docker dev (host runs Postgres separately): defaults work if a Postgres is reachable at `localhost:5432` with user/db `tso`. Otherwise override via env vars.

`infra/docker-compose.yml` already starts Postgres with healthcheck; the catalog service already depends on `postgres: service_healthy`. No compose changes needed.

## 10. Pom additions

`services/catalog/pom.xml`:

- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.flywaydb:flyway-core`
- `org.flywaydb:flyway-database-postgresql`
- `org.postgresql:postgresql` (runtime)
- `org.testcontainers:junit-jupiter` (test)
- `org.testcontainers:postgresql` (test)

Spring Boot's BOM manages Testcontainers versions; we only need to pin the BOM via `<dependencyManagement>` in `services/pom.xml` if it isn't already (verify during implementation).

## 11. README

Update `services/catalog/README.md` with:

- Schema overview (one-paragraph description of `show` / `episode`).
- How to run the seed script and regenerate a migration.
- Attribution block: "Episode summaries are derived from Wikipedia and licensed under CC BY-SA 3.0. Source article URLs are listed in the trailing comment of each `V*__seed_*.sql` migration."

## 12. Definition of Done

- [ ] `./mvnw -pl catalog verify` passes locally and in CI.
- [ ] `docker compose -f infra/docker-compose.yml up --build` boots the catalog service, applies V1 and V2 migrations, and serves the two endpoints.
- [ ] `GET /catalog/shows` returns one show; `GET /catalog/shows/{id}/episodes` returns 42 episodes.
- [ ] `api/openapi.yaml` describes both endpoints; `./api/scripts/gen-all.sh` produces no diff after the edits.
- [ ] `services/catalog/README.md` reflects the schema + seed workflow with attribution.
- [ ] Spotless / lint clean.
- [ ] PR description links back to this spec.

## 13. Open questions for the team

1. Should the seed migration be checked in as `V2__seed_stranger_things.sql` (couples migration filename to show name) or `V2__seed_initial_show.sql` (show-agnostic, easier to grep)? Recommendation: name-specific — explicit, and we'll have one per show anyway.
2. Where should the seed script live longer-term — `services/catalog/scripts/` (close to the migration it produces, current proposal) or a top-level `tools/` directory if other services need similar ingestion? Recommendation: keep it next to the migration until a second use case appears.
