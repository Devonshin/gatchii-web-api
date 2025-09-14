# Gatchii Web API

A Kotlin/Ktor-based web API for authentication and token management for the Gatchii ecosystem. It provides:
- Login to obtain an access token and a refresh token
- Refresh-token renewal endpoint
- A public JWKS endpoint for validating issued tokens
- Basic authenticated sample route

The service uses Ktor (Netty) with Koin DI, Exposed ORM, HikariCP, and PostgreSQL in normal runtime, with H2 in tests.

## Tech stack
- Language: Kotlin (JVM)
- Runtime: JDK 21
- Framework: Ktor 2.3.x (Netty engine)
- DI: Koin 4.x
- DB/ORM: Exposed, HikariCP
- DB: PostgreSQL (runtime), H2 (tests)
- Logging: Logback
- JSON: kotlinx.serialization
- JWT/JWK: nimbus-jose-jwt, jose4j
- Caching (optional/example): ktor-simple-cache with Redis (disabled by default)
- Build/Package manager: Gradle (Kotlin DSL) via Gradle Wrapper

Relevant versions are declared in gradle.properties.

## Entry points
- Main class: io.ktor.server.netty.EngineMain (configured by Ktor plugin)
- Ktor module: com.gatchii.ApplicationKt.module (wired in application.conf)
- Source entry file: src/main/kotlin/com/gatchii/Application.kt

## Routes (high-level)
- GET /.well-known/gatchii-jwks.json → Public JWKS
- POST /login/attempt → Login, returns access/refresh tokens
- POST /refresh-token/renewal → Requires refresh token (Authorization header); returns a new access token
- GET /authenticated   → Example authenticated endpoint (requires access token)

Note: Security is configured via two JWT auth configurations: "auth-jwt" (access) and "refresh-jwt" (refresh).

## Requirements
- JDK 21+
- Gradle Wrapper (included) — no need to install Gradle
- PostgreSQL (for dev/local/prod runtime), or provide DB env vars
- Optional: Redis (only if you enable the SimpleCache plugin in plugins/HTTP.kt)

## Configuration
Configuration uses HOCON files under src/main/resources and can also be overridden via environment variables.

Profiles/config files:
- application.conf (default, prod)
- application-dev.conf
- application-local.conf
- application-test.conf (used automatically by tests)

Server port:
- application.conf: 80 (overridable via PORT)
- application-dev.conf: 10000 (overridable via PORT)
- application-local.conf: 80 (overridable via PORT)
- application-test.conf: 8880

Environment variables (read by application.conf):
- PORT — HTTP port (optional; overrides config file)
- DB_URL — JDBC URL (e.g., jdbc:postgresql://localhost:5432/gatchii-db)
- DB_USERNAME — database user
- DB_PASSWORD — database password
- DB_DRIVER — JDBC driver class name (e.g., org.postgresql.Driver)

Dependency repository credentials (if needed):
- The build includes a GitHub Packages repo for com.gatchii:gatchii-common-util. If resolution fails, you may need to provide:
  - GITHUB_USERNAME
  - GITHUB_TOKEN (with read:packages)
  Currently, the credentials block is commented in build.gradle.kts. TODO: Document the exact requirement or make the package public.

Secrets/keys:
- There is a `secret` config block in application.conf and some PEM files under secret/ and gatchii_secret/. The current runtime code does not directly read these values; JWT verification is performed using remote JWKS (jwkIssuer) and JWK providers. TODO: Document how these keys are intended to be used (if at all in runtime) or remove them if obsolete.

Database:
- Database settings are read from the `database` section in the active config file.
- At startup, Exposed SchemaUtils creates missing tables/columns for: LoginTable, RsaTable, JwkTable.

## How to run
Use the Gradle Wrapper from the project root.

Development profile:
- ./gradlew run -Pdev

Local profile:
- ./gradlew run -Plocal

Default (prod) profile:
- Ensure DB_* env vars are set (see above)
- ./gradlew run

Notes:
- Profiles are selected via project properties in build.gradle.kts (dev, test, local) which set -Dconfig.resource accordingly. Without a profile, application.conf is used.
- If the run task is not available in your environment, you can build and run the distribution: TODO verify and document packaging task(s) (e.g., installDist, buildFatJar).

## Testing
- Run all tests: ./gradlew test
- Unit tests only: ./gradlew unitTest
- Integration tests only: ./gradlew integrationTest

JUnit 5 is used with custom tags via annotations in src/test/kotlin/shared/common:
- @UnitTest (tag: unitTest)
- @IntegrationTest (tag: integrationTest)

Tests automatically use application-test.conf.

## Useful Gradle tasks
- build — Compile and package
- test — Run all tests (uses JUnit Platform)
- unitTest — Run tests tagged with @UnitTest
- integrationTest — Run tests tagged with @IntegrationTest

## Project structure (selected)
- build.gradle.kts — Gradle build script (Ktor plugin, dependencies, tasks)
- gradle.properties — Version catalog for dependencies
- src/main/kotlin/com/gatchii/Application.kt — Main Ktor module loader
- src/main/kotlin/com/gatchii/plugins/ — Ktor feature wiring (DB, Routing, Security, Serialization, etc.)
- src/main/kotlin/com/gatchii/domain/** — Domain layers (login, jwt, jwk, rsa) with repositories/services/routes
- src/main/resources/application*.conf — Environment-specific configuration
- src/test/kotlin/** — Unit and integration tests, test utilities
- docs/flowchart/*.svg — Architecture/flow diagrams

## Diagrams
- docs/flowchart/jwk-strategy.drawio.svg
- docs/flowchart/jwt-process.drawio.svg
- docs/flowchart/login-strategy.drawio.svg

## Logging
- Logback is configured via src/main/resources/logback.xml (and logback-test.xml for tests). Logs are written under logs/ with rotation (see logs/bak/ examples).

## Contributing
TODO: Define contributing guidelines, code style, and branching strategy if external contributions are expected.

## Notes / TODOs
- Verify and document packaging/distribution tasks (fat JAR, installDist, Docker image if any)
- Confirm whether GitHub Packages credentials are required for gatchii-common-util
- Clarify intended use of secret PEM files vs remote JWKS
- Consider adding OpenAPI/Swagger for route documentation
