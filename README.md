# Repository Registrar

A central registry for discovering, tracking, and monitoring repositories in a microservice ecosystem. Built with **Java 21** and **Maven** — no frameworks, just the JDK's built-in HTTP server, SQLite, and Jackson.

## Live Demo

**[Open API Explorer →](http://localhost:${PORT:-8080}/swagger-ui)**

The demo starts with six pre-seeded services (payment, order, inventory, notification, analytics, user) in various health states so you can explore the API immediately.

## What it does

| Concern | How |
|---|---|
| **Discovery** | Register any repository with its purpose, type, and exposed APIs |
| **Classification** | Tag repositories as `producer`, `consumer`, or `both` |
| **Health tracking** | Repositories self-report via `POST /repositories/{id}/heartbeat` |
| **API catalogue** | Each repository declares its endpoints (path, method, description) |
| **HATEOAS** | Every response includes `_links` so clients can navigate without hardcoded URLs |
| **Ecosystem stats** | `GET /stats` gives counts by type, health status, and total APIs exposed |

## Running locally

```bash
mvn package -q
java -jar target/repo-registrar-1.0.0.jar
# open http://localhost:8080/swagger-ui
```

## API overview

```
GET    /swagger-ui                       Interactive API explorer
GET    /openapi.json                     OpenAPI 3.0 spec

GET    /repositories                     List all (filter: ?repoType= &healthStatus= &tag=)
POST   /repositories                     Register a repository
GET    /repositories/{id}                Get details
PUT    /repositories/{id}                Update metadata
DELETE /repositories/{id}                Deregister

GET    /repositories/{id}/health         Health status
POST   /repositories/{id}/heartbeat      Self-report health  { "healthStatus": "healthy" }

GET    /stats                            Ecosystem-wide counts
```

## Register example

```bash
curl -s -X POST http://localhost:8080/repositories \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "shipping-service",
    "description": "Manages shipments and emits delivery events",
    "repoType": "both",
    "tags": ["shipping", "logistics"],
    "apis": [
      { "path": "/shipments", "method": "POST", "description": "Create shipment" },
      { "path": "/shipments/{id}/track", "method": "GET", "description": "Track shipment" }
    ]
  }'
```

## Tech stack

- **Java 21** — virtual threads (`Executors.newVirtualThreadPerTaskExecutor`) for concurrency
- **`com.sun.net.httpserver`** — built-in HTTP server, zero dependencies for serving
- **SQLite + JDBC** — embedded persistence via `sqlite-jdbc`
- **Jackson** — JSON serialisation/deserialisation
- **Swagger UI** — loaded from CDN, served at `/swagger-ui`
- **Maven Shade Plugin** — produces a single executable fat JAR

## Deploying

```bash
# Docker
docker build -t repo-registrar .
docker run -p 8080:8080 repo-registrar
```

Deploy the image to [Railway](https://railway.app), [Render](https://render.com), or [Fly.io](https://fly.io) — all have free tiers and can deploy directly from a `Dockerfile`. Update the **Live Demo** link above once deployed.
