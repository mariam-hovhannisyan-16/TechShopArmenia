# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

TechShopArmenia is a Java 21 / Spring Boot 3.3.5 microservices e-commerce backend, built as a multi-module Maven reactor. Each `techshop-*` module is an independently deployable Spring Boot service with its own Postgres database (all hosted in one shared Postgres instance, one DB per service — see `init.sql`). Services communicate synchronously via REST clients and asynchronously via Kafka events.

Modules:
- `techshop-common` — shared library: DTOs (`dto/request`, `dto/response`), domain enums, Kafka event payloads (`event/`), base exception handling, `CurrentUser` security helper. Every service module depends on this.
- `techshop-user` (port 8081) — auth, registration, JWT issuance.
- `techshop-product` (port 8084) — catalog, categories, price history, product variants (color/storage).
- `techshop-cart` (port 8082) — shopping cart, depends on `techshop-product`.
- `techshop-order` (port 8083) — checkout, order lifecycle, reviews, admin stats, payments. Depends on cart/user/product.
- `techshop-notification` (port 8085) — email + in-app notifications, dispatched off Kafka events.
- `techshop-wishlist` (port 8086) — wishlist, depends on `techshop-product`.
- `techshop-chat` (port 8087) — customer chat with optional AI auto-reply (Anthropic API).

A frontend lives separately at `~/WebstormProjects/frontend` (not part of this repo).

## Architecture patterns

**Inter-service communication:**
- Synchronous calls use `*Client` classes (e.g. `techshop-cart/.../client/ProductClient.java`, `techshop-order/.../client/{CartClient,ProductClient,UserClient}.java`) calling other services' REST APIs directly via their `*_SERVICE_URL` (set in `docker-compose.yml`).
- Privileged service-to-service endpoints are protected by `InternalApiKeyGuard` (present in `techshop-product` and `techshop-wishlist`), which does a constant-time comparison against `internal.api-key` (env `INTERNAL_API_KEY`) — not JWT-based.
- Asynchronous flows use Kafka. Producers live in each module's `kafka/*EventProducer.java`, publishing shared event types from `techshop-common/.../event/`. Consumers live in `kafka/*EventConsumer.java` and are annotated with `@KafkaListener(topics = ..., groupId = ..., properties = {"spring.json.value.default.type=..."})`. Example: `techshop-user` publishes `user-deleted` on account deletion; `techshop-cart`, and others, consume it to cascade the deletion.

**Auth:** `techshop-user` issues JWTs (`JwtService`); every other module carries its own `JwtAuthFilter`/`JwtService` copy to validate the same token (shared `JWT_SECRET`) rather than depending on a shared library class — check the local module's `security/` package, not `techshop-common`, when touching JWT logic.

**Exception handling:** each module has a `*ExceptionHandler extends BaseExceptionHandler` (`techshop-common/.../exception/BaseExceptionHandler.java`). Module-specific business exceptions (e.g. `ProductNotFoundException`) extend `TechShopException`, which carries an HTTP status code used directly in the response.

**Payments (`techshop-order`):** `PaymentProviderFactory` resolves a `PaymentProvider` by `PaymentMethod` enum from a `List<PaymentProvider>` injected by Spring. Providers (`IdramPaymentService`, `TelcellPaymentService`, `RoketLinePaymentService`) extend `AbstractSandboxPaymentProvider` and currently only implement sandbox/mock `createPayment`/`verifyPayment` — no real merchant integration exists yet (see `.env.example`).

**Persistence:** JPA + Liquibase per service. Changelogs live at `<module>/src/main/resources/db/changelog/db.changelog-master.xml`, referencing numbered files under `db/changelog/changes/`. Add new schema changes as a new numbered changelog file — don't edit applied ones. `techshop-cart` and `techshop-notification` currently have no changelog (schema managed elsewhere/simple enough for `ddl-auto`).

**Mapping:** MapStruct mappers (`mapper/*Mapper.java`) convert between entities and `techshop-common` DTOs.

## Common commands

Build and test everything (from repo root, this is what CI runs):
```
mvn --batch-mode clean test
```

Build/test a single module (and its dependency, `techshop-common`, must be installed first if not already built in this reactor run):
```
mvn -pl techshop-product -am test
```

Run a single test class or method:
```
mvn -pl techshop-order test -Dtest=OrderServiceImplTest
mvn -pl techshop-order test -Dtest=OrderServiceImplTest#someMethodName
```

Package all modules into runnable jars (skips tests, matches what CI does before deploy):
```
mvn --batch-mode clean package -DskipTests
```

Run the full stack locally via Docker Compose (Postgres, Kafka/Zookeeper, and all 7 services):
```
docker compose up -d --build
```
Copy relevant values from `.env.example` into your shell/`.env` first. Each service's Dockerfile build context is its own module directory and expects `target/*.jar` to already exist there, so `mvn package` (or the Docker build's own Maven stage) must produce it.

Run a single service locally against its own Postgres/Kafka (e.g. for IDE debugging), pointing at the Dockerized infra:
```
cd techshop-user && mvn spring-boot:run
```

## Notable conventions

- Tests: integration tests are named `*IntegrationTest`, migration-behavior tests are named `*MigrationTest` (e.g. `techshop-order/.../AddLanguageToOrdersMigrationTest.java`), unit tests follow `*Test`. H2 is used as the test datasource (see each module's test dependencies).
- `application.yml` config values are always `${ENV_VAR:default}` — check `.env.example` for the full list of environment variables consumed across modules and what they're for.
- `FRONTEND_URL` is used directly for CORS allow-origin in every service; there's no wildcard fallback, so a missing/wrong value silently breaks all browser requests against a real deployment.
- CI/CD (`.github/workflows/deploy.yml`): every push/PR runs the full `mvn test` reactor build. On push to `main` only, it builds jars and deploys over SSH to an EC2 host, diffing changed module directories since the last deployed commit to decide which Docker services to rebuild (a change under `techshop-common/`, root `pom.xml`, or `docker-compose.yml` triggers a full rebuild of all services).
