# API Gateway Rate Limiter

API Gateway Rate Limiter is a Spring Boot API gateway style project that protects selected API routes with JWT support, Redis-backed token-bucket rate limiting, PostgreSQL request logging, and a small analytics dashboard.

## Open-Source Summary

This project is meant to be reusable and easy to understand. It shows how to:

- protect API traffic with a token bucket rate limiter
- store request logs in PostgreSQL
- expose analytics for request activity
- keep dashboard and analytics endpoints available while limiting only audit traffic

## Problem It Solves

APIs can be flooded by repeated requests from the same client, which can slow down the app and make analytics or dashboards unreliable. This project solves that by rate-limiting only the audit POST routes while still allowing the rest of the API to keep working normally.

## What This Project Uses

- Spring Boot 4
- Spring Web MVC interceptors
- Spring Security
- PostgreSQL for API request logs
- Redis for rate limiting state
- Thymeleaf for the dashboard UI

## Rate Limiting

This project uses a token bucket rate limiter.

How it works:

- Each protected key starts with a fixed number of tokens.
- Every allowed request consumes one token.
- Tokens are refilled after a fixed interval.
- When no tokens are left, the request is blocked with HTTP 429.
- Bucket state is stored in Redis so the limiter can track requests consistently.

Where it applies:

- Rate limiting is applied only to the audit POST routes under `/api/rate-limit-demo/audit/**`.
- Analytics and dashboard-related routes are left open so the UI can still fetch data.

Why rate limiting is used:

- It protects the API from abuse and repeated bursts.
- It prevents one client from consuming all available capacity.
- It keeps analytics and dashboard calls responsive.
- It lets the audit endpoints be tested safely without affecting the rest of the application.

## Features

- JWT authentication support
- Token-bucket rate limiting for audit endpoints
- IP and user-based throttling
- API request logging to PostgreSQL
- Analytics summary for recent request activity
- Dashboard UI for testing the limiter

## Project Structure

- `config/` - Spring MVC, security, Redis, and application wiring
- `controller/` - REST and UI endpoints
- `interceptor/` - logging and rate-limiting interceptors
- `service/` - request logging, JWT, analytics, and limiter services
- `model/` - JPA entities
- `repository/` - database access layer

## Prerequisites

- Java 21
- Maven Wrapper (`./mvnw`)
- Docker
- A running PostgreSQL database
- A running Redis container

## Redis Setup

Start the Redis container before running the app:

```bash
sudo docker start redis-gateway
```

If the container does not exist yet, create it first:

```bash
sudo docker pull redis:7
sudo docker run -d --name redis-gateway -p 6379:6379 redis:7
```

## PostgreSQL Setup

Set your local database connection values before starting the app:

```bash
export DB_URL='<your-postgres-jdbc-url>'
export DB_USER='<your-postgres-username>'
export DB_PWD='<your-postgres-password>'
```

## Run The Application

Start Redis and then run the application:

```bash
sudo docker start redis-gateway
export DB_URL='<your-postgres-jdbc-url>'
export DB_USER='<your-postgres-username>'
export DB_PWD='<your-postgres-password>'
./mvnw spring-boot:run
```

One-line version:

```bash
sudo docker start redis-gateway && export DB_URL='<your-postgres-jdbc-url>' DB_USER='<your-postgres-username>' DB_PWD='<your-postgres-password>' && ./mvnw spring-boot:run
```

## How To Use It

1. Open the dashboard at `/dashboard`.
2. Use the rate-limit demo endpoints to consume tokens.
3. Trigger the audit POST endpoint until the limiter returns HTTP 429.
4. Open the analytics page to view stored request counts.

## Useful Endpoints

- `GET /dashboard` - dashboard UI
- `GET /api/analytics/summary` - analytics summary
- `GET /api/rate-limit-demo/config` - limiter configuration
- `GET /api/rate-limit-demo/state` - current token bucket state
- `POST /api/rate-limit-demo/reset` - reset the token bucket
- `POST /api/rate-limit-demo/audit/request` - audit request that is rate limited
- `POST /api/rate-limit-demo/audit/reset` - audit reset that is rate limited

## Screenshots

Add images to `docs/images/` and reference them directly in Markdown:

![Dashboard](docs/images/1.png)
![Rate limiter](docs/images/2.png)
![Rate limiter](docs/images/3.png)
![Rate limiter](docs/images/4.png)

## Validation

I verified the project with these checks:

- `./mvnw test`
- `export DB_URL='<your-postgres-jdbc-url>' DB_USER='<your-postgres-username>' DB_PWD='<your-postgres-password>' && ./mvnw spring-boot:run`

## Notes

- API request logs are stored in PostgreSQL.
- Rate limiting applies only to the audit POST routes.
- Analytics and other frontend API calls remain available for the dashboard.