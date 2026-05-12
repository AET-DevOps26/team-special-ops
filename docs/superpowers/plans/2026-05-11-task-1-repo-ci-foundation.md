# Task 1: Repo + CI Foundation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up the mono-repo structure, OpenAPI-first codegen, four runnable service skeletons (3 Spring Boot + 1 FastAPI), a React web client, and a GitHub Actions CI pipeline — so every subsequent backlog item builds on a working foundation.

**Architecture:** Single OpenAPI 3.1 spec in `api/` is the source of truth. Codegen produces Java interfaces, a Python client, and TS types into gitignored output directories. Each service is a real Spring Boot 3 / FastAPI / React project that exposes a `/health` endpoint and runs a passing test. CI runs `spec-lint` first, then a parallel matrix of build/test/lint per service.

**Tech Stack:** Java 21 + Spring Boot 3.x + Maven; Python 3.12 + FastAPI + uv + ruff; React + Vite + TypeScript + Tailwind + pnpm; GitHub Actions; Redocly CLI; openapi-generator; openapi-python-client; openapi-typescript.

---

## File Structure

```
team-special-ops/
├── api/
│   ├── openapi.yaml                                # [Task 2]
│   └── scripts/
│       ├── gen-all.sh                              # [Task 3]
│       └── py-config.json                          # [Task 3]
├── services/
│   ├── user-progress/                              # [Task 4]
│   │   ├── pom.xml
│   │   ├── mvnw, mvnw.cmd, .mvn/
│   │   ├── src/main/java/com/tso/userprogress/
│   │   │   ├── UserProgressApplication.java
│   │   │   └── health/HealthController.java
│   │   ├── src/main/resources/application.yml
│   │   ├── src/test/java/com/tso/userprogress/health/HealthControllerTest.java
│   │   └── README.md
│   ├── catalog/                                    # [Task 5] (same shape as user-progress)
│   ├── chat/                                       # [Task 6] (same shape as user-progress)
│   └── genai/                                      # [Task 7]
│       ├── pyproject.toml
│       ├── uv.lock
│       ├── src/genai/{__init__.py, main.py}
│       ├── tests/{__init__.py, test_health.py}
│       └── README.md
├── web-client/                                     # [Task 8]
│   ├── package.json, pnpm-lock.yaml, tsconfig*.json
│   ├── vite.config.ts, tailwind.config.js, postcss.config.js
│   ├── .eslintrc.cjs, .prettierrc
│   ├── index.html
│   ├── src/{main.tsx, App.tsx, index.css, __tests__/App.test.tsx}
│   └── README.md
├── infra/                                          # [Task 1] placeholder
│   └── .gitkeep
├── docs/
│   ├── problem-statement.md                        # [Task 1] moved from root
│   ├── system-architecture.md                      # [Task 1] moved from root
│   ├── diagrams/                                   # [Task 1] moved from root
│   ├── branch-protection.md                        # [Task 9]
│   └── superpowers/
│       ├── specs/2026-05-11-task-1-...-design.md   # already exists
│       └── plans/2026-05-11-task-1-...md           # this file
├── .github/
│   ├── workflows/ci.yml                            # [Task 11]
│   └── pull_request_template.md                    # [Task 10]
├── .gitignore                                      # [Task 1]
├── .editorconfig                                   # [Task 1]
├── .pre-commit-config.yaml                         # [Task 9]
├── README.md                                       # [Task 12]
└── (problem-statement.md, system-architecture.md, diagrams/ — moved out in Task 1)
```

---

## Task 1: Repo restructure — move docs, create skeleton dirs, root hygiene files

**Files:**
- Move: `problem-statement.md` → `docs/problem-statement.md`
- Move: `system-architecture.md` → `docs/system-architecture.md`
- Move: `diagrams/` → `docs/diagrams/`
- Create: `infra/.gitkeep`
- Create: `.gitignore`
- Create: `.editorconfig`

- [ ] **Step 1: Create new directory structure**

```bash
cd /Users/atharva/uni/devops/team-special-ops
mkdir -p api/scripts services web-client infra .github/workflows docs
```

- [ ] **Step 2: Move existing docs into `docs/`**

```bash
git mv problem-statement.md docs/problem-statement.md
git mv system-architecture.md docs/system-architecture.md
git mv diagrams docs/diagrams
touch infra/.gitkeep
```

- [ ] **Step 3: Update relative image links in docs**

The moved docs reference `./diagrams/...`. After moving into `docs/`, paths still resolve because both moved together. Verify with:

```bash
grep -n 'diagrams/' docs/system-architecture.md
```

Expected: all references read `./diagrams/...` and resolve to `docs/diagrams/...`. No changes needed.

- [ ] **Step 4: Create `.gitignore`**

Write `/Users/atharva/uni/devops/team-special-ops/.gitignore`:

```
# Generated code (regen via api/scripts/gen-all.sh)
services/*/generated/
web-client/src/api/types.ts

# Java
target/
*.class
.mvn/wrapper/maven-wrapper.jar
!.mvn/wrapper/maven-wrapper.properties

# Python
.venv/
__pycache__/
*.pyc
.pytest_cache/
.ruff_cache/

# Node / frontend
node_modules/
dist/
.vite/

# IDEs
.idea/
.vscode/
*.iml

# OS
.DS_Store
Thumbs.db

# Env
.env
.env.local
```

- [ ] **Step 5: Create `.editorconfig`**

Write `/Users/atharva/uni/devops/team-special-ops/.editorconfig`:

```
root = true

[*]
end_of_line = lf
insert_final_newline = true
charset = utf-8
trim_trailing_whitespace = true
indent_style = space
indent_size = 2

[*.java]
indent_size = 4

[*.py]
indent_size = 4

[Makefile]
indent_style = tab
```

- [ ] **Step 6: Verify**

```bash
ls -la
ls docs/
```

Expected: root contains `api/`, `services/`, `web-client/`, `infra/`, `docs/`, `.github/`, `.gitignore`, `.editorconfig`, `README.md`. `docs/` contains `problem-statement.md`, `system-architecture.md`, `diagrams/`. Originals no longer at root.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "chore: restructure repo into mono-repo layout

- move problem-statement.md, system-architecture.md, diagrams/ into docs/
- add empty api/, services/, web-client/, infra/, .github/workflows/ dirs
- add root .gitignore and .editorconfig"
```

---

## Task 2: Write `api/openapi.yaml`

**Files:**
- Create: `api/openapi.yaml`

- [ ] **Step 1: Write the spec**

Write `/Users/atharva/uni/devops/team-special-ops/api/openapi.yaml`:

```yaml
openapi: 3.1.0
info:
  title: Spoiler-Safe TV Q&A API
  version: 0.1.0
  description: |
    Single source of truth. v0 contains only health endpoints; real endpoints
    are added in subsequent backlog items.

servers:
  - url: http://localhost:8081
    description: user-progress (local)
  - url: http://localhost:8082
    description: catalog (local)
  - url: http://localhost:8083
    description: chat (local)
  - url: http://localhost:8084
    description: genai (local)

paths:
  /user-progress/health:
    get:
      tags: [user-progress]
      operationId: userProgressHealth
      summary: Health check
      responses:
        '200':
          $ref: '#/components/responses/Health'

  /catalog/health:
    get:
      tags: [catalog]
      operationId: catalogHealth
      summary: Health check
      responses:
        '200':
          $ref: '#/components/responses/Health'

  /chat/health:
    get:
      tags: [chat]
      operationId: chatHealth
      summary: Health check
      responses:
        '200':
          $ref: '#/components/responses/Health'

  /genai/health:
    get:
      tags: [genai]
      operationId: genaiHealth
      summary: Health check
      responses:
        '200':
          $ref: '#/components/responses/Health'

components:
  responses:
    Health:
      description: Service is up
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/HealthStatus'

  schemas:
    HealthStatus:
      type: object
      required: [status, service]
      properties:
        status:
          type: string
          enum: [ok]
        service:
          type: string
          example: catalog

    Error:
      type: object
      required: [code, message]
      properties:
        code:
          type: string
          example: VALIDATION_FAILED
        message:
          type: string
        details:
          type: object
          additionalProperties: true
```

- [ ] **Step 2: Lint the spec**

```bash
cd /Users/atharva/uni/devops/team-special-ops
pnpm dlx @redocly/cli@latest lint api/openapi.yaml
```

Expected: `Woohoo! Your API description is valid.` (warnings about missing `description` fields on tags are fine and can be ignored.)

If you get errors, fix them in the YAML and re-run.

- [ ] **Step 3: Commit**

```bash
git add api/openapi.yaml
git commit -m "feat(api): add v0 OpenAPI spec with /health endpoints

Single source of truth covering all four services. Includes unified
Error schema for downstream tasks."
```

---

## Task 3: Codegen script + Python codegen config

**Files:**
- Create: `api/scripts/gen-all.sh`
- Create: `api/scripts/py-config.json`

- [ ] **Step 1: Write the Python codegen config**

Write `/Users/atharva/uni/devops/team-special-ops/api/scripts/py-config.json`:

```json
{
  "project_name_override": "genai-api-client",
  "package_name_override": "genai_api_client"
}
```

- [ ] **Step 2: Write the codegen script**

Write `/Users/atharva/uni/devops/team-special-ops/api/scripts/gen-all.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

# Run from anywhere; cd to repo root.
cd "$(dirname "$0")/../.."

echo "→ Validating OpenAPI spec..."
pnpm dlx @redocly/cli@latest lint api/openapi.yaml

echo "→ Generating Java stubs for Spring services..."
declare -A JAVA_PKG=( [user-progress]=userprogress [catalog]=catalog [chat]=chat )
for svc in user-progress catalog chat; do
  pkg="${JAVA_PKG[$svc]}"
  rm -rf "services/$svc/generated"
  pnpm dlx @openapitools/openapi-generator-cli@latest generate \
    -i api/openapi.yaml \
    -g spring \
    -o "services/$svc/generated" \
    --additional-properties="interfaceOnly=true,useTags=true,apiPackage=com.tso.${pkg}.api,modelPackage=com.tso.${pkg}.model,useSpringBoot3=true"
done

echo "→ Generating Python client for GenAI service..."
rm -rf services/genai/generated
pnpm dlx openapi-python-client@latest generate \
  --path api/openapi.yaml \
  --output-path services/genai/generated \
  --config api/scripts/py-config.json \
  --overwrite

echo "→ Generating TypeScript types for web-client..."
mkdir -p web-client/src/api
pnpm dlx openapi-typescript@latest api/openapi.yaml \
  -o web-client/src/api/types.ts

echo "✓ Codegen complete."
```

- [ ] **Step 3: Make the script executable**

```bash
chmod +x /Users/atharva/uni/devops/team-special-ops/api/scripts/gen-all.sh
```

- [ ] **Step 4: Run it and verify**

```bash
cd /Users/atharva/uni/devops/team-special-ops
./api/scripts/gen-all.sh
```

Expected output ends with `✓ Codegen complete.` Verify outputs exist:

```bash
ls services/user-progress/generated/src/main/java/com/tso/userprogress/api/
ls services/genai/generated/genai_api_client/
ls web-client/src/api/types.ts
```

Expected: each path exists. If `services/user-progress/generated/` is missing, the Java codegen failed — re-read the output for the error. (You may need Java installed locally; `pnpm dlx` will download `openapi-generator-cli`, which requires JDK 11+ to run.)

- [ ] **Step 5: Commit (without generated outputs — already gitignored)**

```bash
git status   # should NOT list services/*/generated/ or web-client/src/api/types.ts
git add api/scripts/gen-all.sh api/scripts/py-config.json
git commit -m "feat(api): add codegen script for Java/Python/TS clients

gen-all.sh validates spec, then generates Java interfaces (Spring),
Python client, and TypeScript types. Generated outputs are gitignored."
```

---

## Task 4: `services/user-progress/` — Spring Boot skeleton (TDD)

**Files:**
- Create: `services/user-progress/pom.xml`
- Create: `services/user-progress/mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`
- Create: `services/user-progress/src/main/java/com/tso/userprogress/UserProgressApplication.java`
- Create: `services/user-progress/src/main/java/com/tso/userprogress/health/HealthController.java`
- Create: `services/user-progress/src/main/resources/application.yml`
- Create: `services/user-progress/src/test/java/com/tso/userprogress/health/HealthControllerTest.java`
- Create: `services/user-progress/README.md`

- [ ] **Step 1: Bootstrap Spring Boot skeleton via Spring Initializr**

```bash
cd /Users/atharva/uni/devops/team-special-ops/services
curl -sS https://start.spring.io/starter.tgz \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.4.0 \
  -d javaVersion=21 \
  -d groupId=com.tso \
  -d artifactId=user-progress \
  -d name=user-progress \
  -d packageName=com.tso.userprogress \
  -d dependencies=web,actuator \
  -o starter.tgz
mkdir -p user-progress
tar -xzf starter.tgz -C user-progress
rm starter.tgz
```

Verify:

```bash
ls services/user-progress
```

Expected: `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, `src/`, `HELP.md`, `.gitignore`.

- [ ] **Step 2: Remove the Initializr-generated `.gitignore` and `HELP.md`**

We use the root `.gitignore`. Per-service `.gitignore` adds noise.

```bash
rm services/user-progress/.gitignore services/user-progress/HELP.md
```

- [ ] **Step 3: Add `springdoc-openapi-starter-webmvc-ui` and Spotless plugin to `pom.xml`**

Open `services/user-progress/pom.xml`. Inside `<dependencies>...</dependencies>`, add:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

Inside `<build><plugins>...</plugins></build>`, add (after the Spring Boot Maven plugin):

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.43.0</version>
    <configuration>
        <java>
            <googleJavaFormat>
                <version>1.22.0</version>
                <style>GOOGLE</style>
            </googleJavaFormat>
            <removeUnusedImports/>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
            <phase>verify</phase>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 4: Configure port in `application.yml`**

Rename `services/user-progress/src/main/resources/application.properties` to `application.yml`, replacing contents:

```bash
cd /Users/atharva/uni/devops/team-special-ops
rm services/user-progress/src/main/resources/application.properties
```

Write `services/user-progress/src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: user-progress

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

- [ ] **Step 5: Write the failing test**

Write `services/user-progress/src/test/java/com/tso/userprogress/health/HealthControllerTest.java`:

```java
package com.tso.userprogress.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  void healthReturnsOkAndServiceName() throws Exception {
    mvc.perform(get("/user-progress/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"))
        .andExpect(jsonPath("$.service").value("user-progress"));
  }
}
```

Also delete the auto-generated `UserProgressApplicationTests.java` (it tests context load, which our slice test covers more meaningfully):

```bash
rm services/user-progress/src/test/java/com/tso/userprogress/UserProgressApplicationTests.java
```

- [ ] **Step 6: Run the test — expect failure**

```bash
cd /Users/atharva/uni/devops/team-special-ops/services/user-progress
./mvnw -B test -Dtest=HealthControllerTest
```

Expected: BUILD FAILURE — `HealthController` class not found.

- [ ] **Step 7: Run codegen (to generate the API interface the controller will implement)**

```bash
cd /Users/atharva/uni/devops/team-special-ops
./api/scripts/gen-all.sh
```

Verify the interface exists:

```bash
ls services/user-progress/generated/src/main/java/com/tso/userprogress/api/UserProgressApi.java
```

Expected: file exists.

- [ ] **Step 8: Wire generated sources into Maven**

Inside `services/user-progress/pom.xml`, in `<build>`, before `<plugins>`, add a `<resources>` block is unnecessary. Instead add the `build-helper-maven-plugin` to add the generated sources directory. Add to `<build><plugins>`:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <version>3.6.0</version>
    <executions>
        <execution>
            <id>add-generated-sources</id>
            <phase>generate-sources</phase>
            <goals><goal>add-source</goal></goals>
            <configuration>
                <sources>
                    <source>generated/src/main/java</source>
                </sources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Also add the dependencies the generated code imports. Inside `<dependencies>`:

```xml
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-annotations</artifactId>
    <version>2.2.22</version>
</dependency>
<dependency>
    <groupId>org.openapitools</groupId>
    <artifactId>jackson-databind-nullable</artifactId>
    <version>0.2.6</version>
</dependency>
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>
```

- [ ] **Step 9: Write the controller**

Write `services/user-progress/src/main/java/com/tso/userprogress/health/HealthController.java`:

```java
package com.tso.userprogress.health;

import com.tso.userprogress.api.UserProgressApi;
import com.tso.userprogress.model.HealthStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController implements UserProgressApi {

  @Override
  public ResponseEntity<HealthStatus> userProgressHealth() {
    HealthStatus body = new HealthStatus()
        .status(HealthStatus.StatusEnum.OK)
        .service("user-progress");
    return ResponseEntity.ok(body);
  }
}
```

> Note: if the generated `HealthStatus` uses different setter/builder semantics (some generator versions produce records or simple POJOs), adjust to match what `generated/src/main/java/com/tso/userprogress/model/HealthStatus.java` exposes. Open that file if the import doesn't resolve.

- [ ] **Step 10: Run the test — expect pass**

```bash
cd /Users/atharva/uni/devops/team-special-ops/services/user-progress
./mvnw -B test -Dtest=HealthControllerTest
```

Expected: `BUILD SUCCESS`, 1 test run, 0 failures.

- [ ] **Step 11: Run full verify (includes Spotless check)**

```bash
./mvnw -B verify
```

If Spotless complains about formatting, auto-fix:

```bash
./mvnw spotless:apply
./mvnw -B verify
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 12: Write per-service README**

Write `services/user-progress/README.md`:

```markdown
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
```

- [ ] **Step 13: Commit**

```bash
cd /Users/atharva/uni/devops/team-special-ops
git add services/user-progress
git commit -m "feat(user-progress): bootstrap Spring Boot service with /health

Spring Initializr starter (Boot 3.4, Java 21) + springdoc-openapi for
Swagger UI + Spotless. Controller implements codegen'd UserProgressApi
interface. WebMvcTest asserts /user-progress/health returns 200."
```

---

## Task 5: `services/catalog/` — same shape as user-progress

**Files:**
- Create: `services/catalog/` (full Spring Boot skeleton)

- [ ] **Step 1: Bootstrap**

```bash
cd /Users/atharva/uni/devops/team-special-ops/services
curl -sS https://start.spring.io/starter.tgz \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.4.0 \
  -d javaVersion=21 \
  -d groupId=com.tso \
  -d artifactId=catalog \
  -d name=catalog \
  -d packageName=com.tso.catalog \
  -d dependencies=web,actuator \
  -o starter.tgz
mkdir -p catalog
tar -xzf starter.tgz -C catalog
rm starter.tgz
rm catalog/.gitignore catalog/HELP.md
rm catalog/src/test/java/com/tso/catalog/CatalogApplicationTests.java
```

- [ ] **Step 2: Add springdoc + Spotless + generated-sources plumbing to `pom.xml`**

Open `services/catalog/pom.xml`. Inside `<dependencies>...</dependencies>`, add:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-annotations</artifactId>
    <version>2.2.22</version>
</dependency>
<dependency>
    <groupId>org.openapitools</groupId>
    <artifactId>jackson-databind-nullable</artifactId>
    <version>0.2.6</version>
</dependency>
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>
```

Inside `<build><plugins>...</plugins></build>`, add the Spotless plugin and the build-helper plugin (after the Spring Boot Maven plugin):

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.43.0</version>
    <configuration>
        <java>
            <googleJavaFormat>
                <version>1.22.0</version>
                <style>GOOGLE</style>
            </googleJavaFormat>
            <removeUnusedImports/>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
            <phase>verify</phase>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <version>3.6.0</version>
    <executions>
        <execution>
            <id>add-generated-sources</id>
            <phase>generate-sources</phase>
            <goals><goal>add-source</goal></goals>
            <configuration>
                <sources>
                    <source>generated/src/main/java</source>
                </sources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 3: Replace `application.properties` with `application.yml`**

```bash
rm services/catalog/src/main/resources/application.properties
```

Write `services/catalog/src/main/resources/application.yml`:

```yaml
server:
  port: 8082

spring:
  application:
    name: catalog

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

- [ ] **Step 4: Write the failing test**

Write `services/catalog/src/test/java/com/tso/catalog/health/HealthControllerTest.java`:

```java
package com.tso.catalog.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  void healthReturnsOkAndServiceName() throws Exception {
    mvc.perform(get("/catalog/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"))
        .andExpect(jsonPath("$.service").value("catalog"));
  }
}
```

- [ ] **Step 5: Run test — expect failure**

```bash
cd /Users/atharva/uni/devops/team-special-ops/services/catalog
./mvnw -B test -Dtest=HealthControllerTest
```

Expected: FAIL — `HealthController` not found.

- [ ] **Step 6: Regenerate (codegen) and write controller**

```bash
cd /Users/atharva/uni/devops/team-special-ops
./api/scripts/gen-all.sh
```

Write `services/catalog/src/main/java/com/tso/catalog/health/HealthController.java`:

```java
package com.tso.catalog.health;

import com.tso.catalog.api.CatalogApi;
import com.tso.catalog.model.HealthStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController implements CatalogApi {

  @Override
  public ResponseEntity<HealthStatus> catalogHealth() {
    HealthStatus body = new HealthStatus()
        .status(HealthStatus.StatusEnum.OK)
        .service("catalog");
    return ResponseEntity.ok(body);
  }
}
```

- [ ] **Step 7: Run test — expect pass, then full verify**

```bash
cd /Users/atharva/uni/devops/team-special-ops/services/catalog
./mvnw -B test -Dtest=HealthControllerTest
./mvnw -B verify
```

If Spotless fails: `./mvnw spotless:apply && ./mvnw -B verify`.

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Write per-service README**

Write `services/catalog/README.md`:

```markdown
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
```

- [ ] **Step 9: Commit**

```bash
cd /Users/atharva/uni/devops/team-special-ops
git add services/catalog
git commit -m "feat(catalog): bootstrap Spring Boot service with /health"
```

---

## Task 6: `services/chat/` — same shape as user-progress

**Files:**
- Create: `services/chat/` (full Spring Boot skeleton)

- [ ] **Step 1: Bootstrap**

```bash
cd /Users/atharva/uni/devops/team-special-ops/services
curl -sS https://start.spring.io/starter.tgz \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.4.0 \
  -d javaVersion=21 \
  -d groupId=com.tso \
  -d artifactId=chat \
  -d name=chat \
  -d packageName=com.tso.chat \
  -d dependencies=web,actuator \
  -o starter.tgz
mkdir -p chat
tar -xzf starter.tgz -C chat
rm starter.tgz
rm chat/.gitignore chat/HELP.md
rm chat/src/test/java/com/tso/chat/ChatApplicationTests.java
```

- [ ] **Step 2: Add springdoc + Spotless + generated-sources to `pom.xml`**

Open `services/chat/pom.xml`. Inside `<dependencies>...</dependencies>`, add:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-annotations</artifactId>
    <version>2.2.22</version>
</dependency>
<dependency>
    <groupId>org.openapitools</groupId>
    <artifactId>jackson-databind-nullable</artifactId>
    <version>0.2.6</version>
</dependency>
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
</dependency>
```

Inside `<build><plugins>...</plugins></build>`, add the Spotless plugin and the build-helper plugin (after the Spring Boot Maven plugin):

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.43.0</version>
    <configuration>
        <java>
            <googleJavaFormat>
                <version>1.22.0</version>
                <style>GOOGLE</style>
            </googleJavaFormat>
            <removeUnusedImports/>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
            <phase>verify</phase>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <version>3.6.0</version>
    <executions>
        <execution>
            <id>add-generated-sources</id>
            <phase>generate-sources</phase>
            <goals><goal>add-source</goal></goals>
            <configuration>
                <sources>
                    <source>generated/src/main/java</source>
                </sources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 3: Replace `application.properties` with `application.yml`**

```bash
rm services/chat/src/main/resources/application.properties
```

Write `services/chat/src/main/resources/application.yml`:

```yaml
server:
  port: 8083

spring:
  application:
    name: chat

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

- [ ] **Step 4: Write the failing test**

Write `services/chat/src/test/java/com/tso/chat/health/HealthControllerTest.java`:

```java
package com.tso.chat.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  void healthReturnsOkAndServiceName() throws Exception {
    mvc.perform(get("/chat/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"))
        .andExpect(jsonPath("$.service").value("chat"));
  }
}
```

- [ ] **Step 5: Run test — expect failure**

```bash
cd /Users/atharva/uni/devops/team-special-ops/services/chat
./mvnw -B test -Dtest=HealthControllerTest
```

Expected: FAIL.

- [ ] **Step 6: Regenerate and write controller**

```bash
cd /Users/atharva/uni/devops/team-special-ops
./api/scripts/gen-all.sh
```

Write `services/chat/src/main/java/com/tso/chat/health/HealthController.java`:

```java
package com.tso.chat.health;

import com.tso.chat.api.ChatApi;
import com.tso.chat.model.HealthStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController implements ChatApi {

  @Override
  public ResponseEntity<HealthStatus> chatHealth() {
    HealthStatus body = new HealthStatus()
        .status(HealthStatus.StatusEnum.OK)
        .service("chat");
    return ResponseEntity.ok(body);
  }
}
```

- [ ] **Step 7: Run test — expect pass, then full verify**

```bash
cd /Users/atharva/uni/devops/team-special-ops/services/chat
./mvnw -B test -Dtest=HealthControllerTest
./mvnw -B verify
```

If Spotless fails: `./mvnw spotless:apply && ./mvnw -B verify`.

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Write per-service README**

Write `services/chat/README.md`:

```markdown
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
```

- [ ] **Step 9: Commit**

```bash
cd /Users/atharva/uni/devops/team-special-ops
git add services/chat
git commit -m "feat(chat): bootstrap Spring Boot service with /health"
```

---

## Task 7: `services/genai/` — FastAPI + LangChain skeleton (TDD)

**Files:**
- Create: `services/genai/pyproject.toml`
- Create: `services/genai/src/genai/{__init__.py, main.py}`
- Create: `services/genai/tests/{__init__.py, test_health.py}`
- Create: `services/genai/README.md`

- [ ] **Step 1: Bootstrap with uv**

```bash
cd /Users/atharva/uni/devops/team-special-ops
uv init --no-readme --package services/genai
```

This creates `services/genai/pyproject.toml`, `services/genai/src/genai/__init__.py`, and `services/genai/.python-version`.

If `uv` is not installed: `curl -LsSf https://astral.sh/uv/install.sh | sh` (macOS/Linux).

- [ ] **Step 2: Edit `pyproject.toml` to add runtime + dev deps and ruff config**

Replace `services/genai/pyproject.toml` with:

```toml
[project]
name = "genai"
version = "0.1.0"
description = "GenAI service: FastAPI + LangChain"
requires-python = ">=3.12"
dependencies = [
    "fastapi>=0.115.0",
    "uvicorn[standard]>=0.32.0",
    "langchain>=0.3.0",
    "langchain-openai>=0.2.0",
]

[project.optional-dependencies]
dev = [
    "pytest>=8.3.0",
    "pytest-asyncio>=0.24.0",
    "httpx>=0.27.0",
    "ruff>=0.7.0",
]

[build-system]
requires = ["hatchling"]
build-backend = "hatchling.build"

[tool.hatch.build.targets.wheel]
packages = ["src/genai"]

[tool.pytest.ini_options]
asyncio_mode = "auto"
testpaths = ["tests"]

[tool.ruff]
line-length = 100
target-version = "py312"

[tool.ruff.lint]
select = ["E", "F", "I", "B", "UP"]
```

- [ ] **Step 3: Install deps**

```bash
cd /Users/atharva/uni/devops/team-special-ops/services/genai
uv sync --extra dev
```

Expected: `.venv/` created, dependencies installed.

- [ ] **Step 4: Write the failing test**

Write `services/genai/tests/__init__.py` as an empty file.

Write `services/genai/tests/test_health.py`:

```python
from fastapi.testclient import TestClient

from genai.main import app


def test_health_returns_ok_and_service_name():
    client = TestClient(app)
    response = client.get("/genai/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "genai"}
```

- [ ] **Step 5: Run the test — expect failure**

```bash
cd /Users/atharva/uni/devops/team-special-ops/services/genai
uv run pytest -v
```

Expected: ImportError or attribute error — `app` not defined in `genai.main`.

- [ ] **Step 6: Write the minimal implementation**

Replace `services/genai/src/genai/__init__.py` with empty file (or leave whatever `uv init` put there).

Write `services/genai/src/genai/main.py`:

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="GenAI Service", version="0.1.0")


class HealthStatus(BaseModel):
    status: str
    service: str


@app.get("/genai/health", response_model=HealthStatus, tags=["genai"])
async def health() -> HealthStatus:
    return HealthStatus(status="ok", service="genai")
```

- [ ] **Step 7: Run the test — expect pass**

```bash
cd /Users/atharva/uni/devops/team-special-ops/services/genai
uv run pytest -v
```

Expected: `1 passed`.

- [ ] **Step 8: Run lint**

```bash
uv run ruff check .
uv run ruff format --check .
```

Expected: `All checks passed`. If format check fails, run `uv run ruff format .` and re-check.

- [ ] **Step 9: Run the service manually to verify Swagger UI**

```bash
uv run uvicorn genai.main:app --port 8084 &
sleep 2
curl -s http://localhost:8084/genai/health
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8084/docs
kill %1
```

Expected:
- `/genai/health` → `{"status":"ok","service":"genai"}`
- `/docs` → `200`

- [ ] **Step 10: Write per-service README**

Write `services/genai/README.md`:

```markdown
# genai

FastAPI + LangChain service that handles LLM calls.

## Run

    uv sync --extra dev
    uv run uvicorn genai.main:app --port 8084 --reload

Service listens on http://localhost:8084

- Health: http://localhost:8084/genai/health
- Swagger UI: http://localhost:8084/docs
- ReDoc: http://localhost:8084/redoc

## Test

    uv run pytest -v

## Lint

    uv run ruff check .
    uv run ruff format --check .

Regenerate the client from the OpenAPI spec before development if the spec changed:

    ../../api/scripts/gen-all.sh
```

- [ ] **Step 11: Commit**

```bash
cd /Users/atharva/uni/devops/team-special-ops
git add services/genai
git commit -m "feat(genai): bootstrap FastAPI service with /health

uv-managed dependencies, ruff for lint/format, pytest. Health endpoint
matches OpenAPI spec at /genai/health. Swagger UI auto-mounted at /docs."
```

---

## Task 8: `web-client/` — React + Vite + TS skeleton (TDD)

**Files:**
- Create: `web-client/` (full Vite scaffold)
- Modify: `web-client/package.json` (add gen + test scripts; add Tailwind, ESLint, Prettier deps)
- Create: `web-client/tailwind.config.js`, `postcss.config.js`
- Create: `web-client/.eslintrc.cjs`, `web-client/.prettierrc`
- Replace: `web-client/src/App.tsx`, `web-client/src/index.css`
- Create: `web-client/src/__tests__/App.test.tsx`
- Create: `web-client/README.md`

- [ ] **Step 1: Bootstrap Vite scaffold**

```bash
cd /Users/atharva/uni/devops/team-special-ops
pnpm create vite@latest web-client -- --template react-ts
cd web-client
pnpm install
```

(`pnpm create vite` will prompt for confirmation if `web-client` exists. If it complains, `rm -rf web-client` first.)

- [ ] **Step 2: Add Tailwind + Vitest + Testing Library + ESLint config deps**

```bash
cd /Users/atharva/uni/devops/team-special-ops/web-client
pnpm add -D tailwindcss postcss autoprefixer \
  vitest @testing-library/react @testing-library/jest-dom jsdom \
  eslint-config-prettier prettier
pnpm dlx tailwindcss init -p
```

This creates `tailwind.config.js` and `postcss.config.js`.

- [ ] **Step 3: Configure Tailwind content paths**

Replace `web-client/tailwind.config.js`:

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: { extend: {} },
  plugins: [],
}
```

- [ ] **Step 4: Wire Tailwind directives into `src/index.css`**

Replace `web-client/src/index.css` (overwrite Vite's default styles):

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

- [ ] **Step 5: Configure Vitest in `vite.config.ts`**

Replace `web-client/vite.config.ts`:

```ts
/// <reference types="vitest" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/__tests__/setup.ts'],
  },
})
```

Write `web-client/src/__tests__/setup.ts`:

```ts
import '@testing-library/jest-dom'
```

- [ ] **Step 6: Configure ESLint + Prettier**

The Vite template already produces `eslint.config.js` (flat config). Append Prettier compatibility — replace `web-client/eslint.config.js` with:

```js
import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import prettier from 'eslint-config-prettier'

export default tseslint.config(
  { ignores: ['dist', 'src/api/types.ts'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended, prettier],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    },
  },
)
```

Write `web-client/.prettierrc`:

```json
{
  "semi": false,
  "singleQuote": true,
  "trailingComma": "all",
  "printWidth": 100
}
```

- [ ] **Step 7: Update `package.json` scripts**

Open `web-client/package.json`, replace the `"scripts"` block with:

```json
"scripts": {
  "dev": "vite",
  "build": "tsc -b && vite build",
  "lint": "eslint .",
  "format": "prettier --write .",
  "test": "vitest run",
  "gen": "openapi-typescript ../api/openapi.yaml -o src/api/types.ts"
}
```

- [ ] **Step 8: Write the failing test**

Write `web-client/src/__tests__/App.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import App from '../App'

describe('App', () => {
  it('renders the placeholder heading', () => {
    render(<App />)
    expect(screen.getByRole('heading', { name: /TV Q&A/i })).toBeInTheDocument()
  })
})
```

- [ ] **Step 9: Run test — expect failure**

```bash
cd /Users/atharva/uni/devops/team-special-ops/web-client
pnpm test
```

Expected: test fails because the default Vite `App.tsx` doesn't contain "TV Q&A".

- [ ] **Step 10: Replace `App.tsx` with the placeholder**

Replace `web-client/src/App.tsx`:

```tsx
function App() {
  return (
    <main className="min-h-screen flex items-center justify-center bg-slate-50">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-slate-900">TV Q&amp;A</h1>
        <p className="mt-2 text-slate-600">Spoiler-safe answers — coming soon.</p>
      </div>
    </main>
  )
}

export default App
```

Also delete `web-client/src/App.css` if present (no longer used):

```bash
rm -f web-client/src/App.css
```

And remove the `import './App.css'` line from `App.tsx` (already removed above).

- [ ] **Step 11: Run test — expect pass**

```bash
pnpm test
```

Expected: `1 passed`.

- [ ] **Step 12: Run lint and build**

```bash
pnpm lint
pnpm build
```

Both should succeed. Build creates `dist/`.

- [ ] **Step 13: Verify dev server starts**

```bash
pnpm dev &
sleep 3
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:5173/
kill %1
```

Expected: `200`.

- [ ] **Step 14: Write per-service README**

Write `web-client/README.md`:

```markdown
# web-client

React + Vite + TypeScript + Tailwind frontend.

## Run

    pnpm install
    pnpm dev

Dev server: http://localhost:5173

## Test

    pnpm test

## Lint / format

    pnpm lint
    pnpm format

## Regenerate API types from the spec

    pnpm gen
    # or from repo root: ../api/scripts/gen-all.sh
```

- [ ] **Step 15: Commit**

```bash
cd /Users/atharva/uni/devops/team-special-ops
git add web-client
git commit -m "feat(web-client): bootstrap React + Vite + TS + Tailwind

Vitest + Testing Library tests, ESLint + Prettier, Tailwind CSS.
Placeholder UI passes a unit test for the rendered heading."
```

---

## Task 9: Pre-commit hook + branch-protection runbook

**Files:**
- Create: `.pre-commit-config.yaml`
- Create: `docs/branch-protection.md`

- [ ] **Step 1: Write `.pre-commit-config.yaml`**

Write `/Users/atharva/uni/devops/team-special-ops/.pre-commit-config.yaml`:

```yaml
repos:
  - repo: local
    hooks:
      - id: redocly-lint
        name: Redocly OpenAPI lint
        entry: pnpm dlx @redocly/cli@latest lint api/openapi.yaml
        language: system
        files: ^api/openapi\.yaml$
        pass_filenames: false
```

We use the `local` repo with `language: system` so the hook reuses the same Redocly version `gen-all.sh` and CI use (no version drift across pre-commit's own repo).

- [ ] **Step 2: Verify the hook**

```bash
cd /Users/atharva/uni/devops/team-special-ops
pip install --user pre-commit  # or pipx install pre-commit / brew install pre-commit
pre-commit install
pre-commit run --all-files
```

Expected: `Redocly OpenAPI lint........Passed` (or `Skipped` if no matching files staged).

If `pre-commit` is not installed, skip the install step here; document it in the README and let the user install on first use.

- [ ] **Step 3: Write `docs/branch-protection.md`**

Write `/Users/atharva/uni/devops/team-special-ops/docs/branch-protection.md`:

```markdown
# Branch protection setup (admin runbook)

Branch protection is a GitHub setting, not code. The repo admin (Owner role)
must configure it manually after this PR merges.

## Steps

1. Go to **Repo → Settings → Branches**.
2. Click **Add branch protection rule** (or edit the existing rule for `main`).
3. **Branch name pattern:** `main`
4. Tick **Require a pull request before merging**.
   - **Require approvals:** 1
   - **Dismiss stale pull request approvals when new commits are pushed:** ✓
5. Tick **Require status checks to pass before merging**.
   - **Require branches to be up to date before merging:** ✓
   - **Required status checks** (after the first CI run completes, these become
     selectable in the search box):
     - `spec-lint`
     - `java-services (user-progress)`
     - `java-services (catalog)`
     - `java-services (chat)`
     - `genai`
     - `web-client`
6. Tick **Do not allow bypassing the above settings**.
7. Click **Create / Save changes**.

## Verification

Open a throwaway PR that intentionally breaks the spec (e.g., delete a required
field in `api/openapi.yaml`). Confirm:

- `spec-lint` fails.
- The PR cannot be merged.

Revert the change and confirm the same PR becomes mergeable after CI is green.
```

- [ ] **Step 4: Commit**

```bash
git add .pre-commit-config.yaml docs/branch-protection.md
git commit -m "chore: add pre-commit hook for OpenAPI lint + branch-protection runbook"
```

---

## Task 10: PR template

**Files:**
- Create: `.github/pull_request_template.md`

- [ ] **Step 1: Write the template**

Write `/Users/atharva/uni/devops/team-special-ops/.github/pull_request_template.md`:

```markdown
## Summary

<!-- What changed and why? -->

## Checklist

- [ ] Affects the API contract? If yes, `api/openapi.yaml` is updated and `./api/scripts/gen-all.sh` regenerated clean
- [ ] Tests added/updated
- [ ] Swagger UI verified for any new endpoints
- [ ] CI is green
- [ ] Docs updated if user- or ops-facing

## Test plan

<!-- How to verify locally; commands to run, endpoints to hit. -->
```

- [ ] **Step 2: Commit**

```bash
git add .github/pull_request_template.md
git commit -m "chore: add PR template"
```

---

## Task 11: CI workflow

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Write the workflow**

Write `/Users/atharva/uni/devops/team-special-ops/.github/workflows/ci.yml`:

```yaml
name: CI

on:
  pull_request:
  push:
    branches: [main]

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

jobs:
  spec-lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
        with:
          version: 9
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: pnpm
      - run: pnpm dlx @redocly/cli@latest lint api/openapi.yaml

  java-services:
    needs: spec-lint
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        service: [user-progress, catalog, chat]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven
      - uses: pnpm/action-setup@v4
        with:
          version: 9
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: pnpm
      - name: Regenerate stubs from OpenAPI spec
        run: ./api/scripts/gen-all.sh
      - name: Build + test + lint
        working-directory: services/${{ matrix.service }}
        run: ./mvnw -B verify

  genai:
    needs: spec-lint
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.12'
      - uses: astral-sh/setup-uv@v3
      - uses: pnpm/action-setup@v4
        with:
          version: 9
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: pnpm
      - name: Regenerate client from OpenAPI spec
        run: ./api/scripts/gen-all.sh
      - name: Sync dependencies
        working-directory: services/genai
        run: uv sync --extra dev
      - name: Lint
        working-directory: services/genai
        run: |
          uv run ruff check .
          uv run ruff format --check .
      - name: Test
        working-directory: services/genai
        run: uv run pytest -v

  web-client:
    needs: spec-lint
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
        with:
          version: 9
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: pnpm
      - name: Regenerate TS types from OpenAPI spec
        run: ./api/scripts/gen-all.sh
      - name: Install deps
        working-directory: web-client
        run: pnpm install --frozen-lockfile
      - name: Lint
        working-directory: web-client
        run: pnpm lint
      - name: Test
        working-directory: web-client
        run: pnpm test
      - name: Build
        working-directory: web-client
        run: pnpm build
```

- [ ] **Step 2: Sanity-check the YAML locally**

```bash
cd /Users/atharva/uni/devops/team-special-ops
pnpm dlx -p js-yaml@4.1.0 js-yaml .github/workflows/ci.yml > /dev/null
```

Expected: no output, exit code 0. (Parses successfully.)

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add GitHub Actions workflow

Parallel matrix per service. spec-lint runs first and gates the rest.
Each downstream job regenerates from the spec, then builds/tests/lints."
```

---

## Task 12: Root README

**Files:**
- Modify: `README.md` (overwrite the existing one-line README)

- [ ] **Step 1: Replace README**

Replace `/Users/atharva/uni/devops/team-special-ops/README.md`:

```markdown
# team-special-ops

Spoiler-safe TV show Q&A — a web app that answers questions about a show using
only episodes the viewer has already seen. Built as a DevOps course project:
mono-repo, OpenAPI-first, CI/CD, observability, Kubernetes deployment.

## Structure

| Path | What |
|---|---|
| `api/openapi.yaml` | Single source of truth — OpenAPI 3.1 spec |
| `api/scripts/gen-all.sh` | Regenerates Java stubs, Python client, TS types |
| `services/user-progress/` | Spring Boot — auth + watch progress (port 8081) |
| `services/catalog/` | Spring Boot — shows + episodes (port 8082) |
| `services/chat/` | Spring Boot — Q&A orchestration (port 8083) |
| `services/genai/` | FastAPI + LangChain — LLM calls (port 8084) |
| `web-client/` | React + Vite + TS + Tailwind (port 5173) |
| `infra/` | docker-compose, Helm charts (added in later tasks) |
| `docs/` | Architecture, diagrams, runbooks, specs, plans |
| `.github/workflows/ci.yml` | Build + test + lint per service |

## One-time setup

    pre-commit install   # local OpenAPI lint hook

## Quickstart (per-service, until Task 2 adds docker-compose)

    # Regenerate clients/stubs from the spec
    ./api/scripts/gen-all.sh

    # Run a Spring Boot service (example: catalog)
    cd services/catalog && ./mvnw spring-boot:run

    # Run the GenAI service
    cd services/genai && uv sync --extra dev && uv run uvicorn genai.main:app --port 8084 --reload

    # Run the web client
    cd web-client && pnpm install && pnpm dev

## Swagger UI

| Service | URL |
|---|---|
| user-progress | http://localhost:8081/swagger-ui.html |
| catalog | http://localhost:8082/swagger-ui.html |
| chat | http://localhost:8083/swagger-ui.html |
| genai | http://localhost:8084/docs |

## Architecture

See [docs/system-architecture.md](./docs/system-architecture.md) and
[docs/diagrams/](./docs/diagrams/).

## Workflow

- Every change goes through a feature branch and a pull request
- `main` is branch-protected (see [docs/branch-protection.md](./docs/branch-protection.md))
- CI must be green and ≥1 teammate must approve before merge
- API changes start in `api/openapi.yaml`, then run `./api/scripts/gen-all.sh`

## Specs and plans

Design docs live in [docs/superpowers/specs/](./docs/superpowers/specs/).
Implementation plans live in [docs/superpowers/plans/](./docs/superpowers/plans/).
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: rewrite root README

Structure overview, per-service quickstart, Swagger UI URLs, workflow
expectations. Replaces the boilerplate placeholder."
```

---

## Task 13: End-to-end verification

**Files:** (none — verification only)

- [ ] **Step 1: Clean checkout test (simulates new contributor)**

```bash
cd /Users/atharva/uni/devops/team-special-ops
git status        # expect: clean
./api/scripts/gen-all.sh
```

Expected: `✓ Codegen complete.`

- [ ] **Step 2: Build + test each service**

```bash
for svc in user-progress catalog chat; do
  (cd services/$svc && ./mvnw -B verify) || exit 1
done
(cd services/genai && uv sync --extra dev && uv run pytest -v && uv run ruff check . && uv run ruff format --check .) || exit 1
(cd web-client && pnpm install --frozen-lockfile && pnpm lint && pnpm test && pnpm build) || exit 1
echo "✓ All services green"
```

Expected: `✓ All services green`.

- [ ] **Step 3: Smoke-test each running service**

Open four terminals (or run via `&`):

```bash
# Terminal 1
cd services/user-progress && ./mvnw spring-boot:run
# Terminal 2
cd services/catalog && ./mvnw spring-boot:run
# Terminal 3
cd services/chat && ./mvnw spring-boot:run
# Terminal 4
cd services/genai && uv run uvicorn genai.main:app --port 8084
```

Then in a fifth terminal:

```bash
curl -s http://localhost:8081/user-progress/health
echo
curl -s http://localhost:8082/catalog/health
echo
curl -s http://localhost:8083/chat/health
echo
curl -s http://localhost:8084/genai/health
echo
```

Expected output:

```
{"status":"ok","service":"user-progress"}
{"status":"ok","service":"catalog"}
{"status":"ok","service":"chat"}
{"status":"ok","service":"genai"}
```

Also visit each Swagger UI in a browser:
- http://localhost:8081/swagger-ui.html
- http://localhost:8082/swagger-ui.html
- http://localhost:8083/swagger-ui.html
- http://localhost:8084/docs

Each should render and list the `/health` endpoint. Use the "Try it out" button on one of them and confirm it responds 200.

Stop all services after verification.

- [ ] **Step 4: Open a draft PR and confirm CI runs green**

```bash
git checkout -b feat/task-1-repo-ci-foundation
git push -u origin feat/task-1-repo-ci-foundation
```

Then open a PR against `main` via the GitHub UI (or `gh pr create --draft`). Wait for the `CI` workflow to complete.

Expected: all five required jobs (`spec-lint`, three `java-services` matrix runs, `genai`, `web-client`) pass green.

If any job fails: read the log, fix locally, push, repeat.

- [ ] **Step 5: Have the repo admin configure branch protection**

Hand `docs/branch-protection.md` to the repo admin. They configure branch protection per the runbook. Verify with a throwaway PR that intentionally breaks `api/openapi.yaml` — `spec-lint` should block merging.

- [ ] **Step 6: Mark Task 1 done**

The PR's checklist (from the template):
- ✓ Affects the API contract? Yes — initial spec added, codegen regenerates clean
- ✓ Tests added/updated — one passing test per service
- ✓ Swagger UI verified for new endpoints — `/health` reachable on each
- ✓ CI is green
- ✓ Docs updated — README, branch-protection.md

Merge once approved.

---

## Self-review notes

**Spec coverage:** Every section of `docs/superpowers/specs/2026-05-11-task-1-repo-ci-foundation-design.md` is covered by tasks above (layout → Task 1; OpenAPI spec → Task 2; codegen → Task 3; Java services → Tasks 4–6; GenAI → Task 7; web-client → Task 8; pre-commit + branch protection → Task 9; PR template → Task 10; CI → Task 11; README → Task 12; end-to-end verification → Task 13).

**Out of scope:** Dockerfiles, docker-compose, Postgres, auth, real endpoints, Prometheus, Grafana, Kubernetes — all explicitly deferred per the spec.

**Known caveats during implementation:**

- The exact field accessors on generated `HealthStatus` may vary by openapi-generator version. If the controller's `new HealthStatus().status(...).service(...)` chained setters don't compile, open `services/<svc>/generated/src/main/java/com/tso/<pkg>/model/HealthStatus.java` to inspect the actual API and adapt the controller.
- Spring Initializr's exact set of dependency versions changes over time. If Boot 3.4.0 has been superseded (or removed) by the time this runs, use the next available 3.x version in the `bootVersion=` parameter.
- `pnpm create vite` is interactive in some versions; if so, follow the prompts choosing React + TypeScript.
