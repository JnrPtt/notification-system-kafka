# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot 3.5 / Java 21 REST API (`com.jnrptt.notificationsystemkafka`) managing users, monthly budgets and expenses. Domain events are published to Kafka and consumed to send emails. A separate Next.js dashboard lives in `FrontEnd/`. The README (in Spanish) documents the API, env vars and data model in detail.

## Commands

Backend (use the wrapper; on Windows `.\mvnw.cmd`):

```bash
./mvnw spring-boot:run                                   # run API on :8080
./mvnw test                                              # all tests
./mvnw test -Dtest=ExpenseServiceTest                    # one class
./mvnw test -Dtest=ExpenseServiceTest#methodName         # one method
docker compose up postgres zookeeper kafka mailhog       # support services only (for local run)
docker compose up --build                                # full stack incl. app
```

Frontend (`FrontEnd/`, Next.js 16 / React 19, runs on :5173): `npm run dev`, `npm run build`, `npm start`. Copy `.env.local.example` to `.env.local` (`NEXT_PUBLIC_API_URL`, must include `/api/v1`). No lint/test scripts exist.

Local URLs: Swagger UI `/swagger-ui.html`, OpenAPI JSON `/api-docs`, MailHog UI `http://localhost:8025`.

## Architecture

Layered: `controller` -> `service` -> `repository` (Spring Data JPA, PostgreSQL), with `model` entities, `dto` request/response objects, `exception` (central error handler returning a common error body with 400/404/409), and `config` (`CorsConfig`, `OpenApiConfig`).

Event flow (the part that spans multiple files):
- Services publish via `kafka/producer/NotificationProducer` using topic names from `kafka/KafkaTopics`: `user-registered`, `expense-created`, `budget-exceeded`.
- `ExpenseService` on create/update sums the month's expenses for the user+category, compares to the matching `Budget`, and publishes `BudgetExceededEvent` when exceeded.
- Kafka consumers use `@RetryableTopic` (3 attempts, 2s backoff, then DLT) and call `notifications/EmailNotificationService` (Spring Mail). `expense-created` currently has no consumer.

Conventions worth knowing: emails and categories are normalized to lowercase; budget `month` is a `yyyy-MM` string; budgets are unique on (user, category, month).

## Configuration

`application.properties` reads env vars with defaults and optionally imports a root `.env` (`spring.config.import=optional:file:.env[.properties]`). Kafka from the host is `localhost:9094`; inside Docker Compose the app uses `kafka:9092`. Hibernate runs with `ddl-auto=update` (no migrations). CORS origins come from `APP_CORS_ALLOWED_ORIGINS` (defaults to localhost 3000/5173/4200). Tests use H2 and spring-kafka-test; service tests are unit tests plus a context-load test.
