# API Gateway Rate Limiter

A Spring Boot API gateway that protects your backend routes with **JWT authentication**, **token-bucket rate limiting** (Redis), **request logging** (PostgreSQL), and a lightweight **analytics dashboard** (Thymeleaf + Chart.js).

---

## ✨ Features

- **JWT Authentication** – Secure your APIs with JSON Web Tokens.
- **Token-Bucket Rate Limiting** – Per-user and per-IP limits stored in Redis (atomic Lua scripts).
- **Request Logging** – Every request is asynchronously logged to PostgreSQL.
- **Analytics Dashboard** – View total requests, top users, and request trends over time.
- **Centralized Gateway** – One service protects all your backend applications.

---

## 🧠 Problem It Solves

APIs are vulnerable to abuse, spam, and DDoS attacks. Without a rate limiter, a single client can overwhelm your system, degrade performance, and skew analytics. This project provides a **reusable, self-contained gateway** that:

- Enforces rate limits only on selected routes (e.g., audit POST endpoints).
- Keeps analytics and dashboard endpoints always available.
- Demonstrates a clean, maintainable architecture using Spring interceptors, Redis, and JPA.

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| Spring Boot 3.2+ | Application framework |
| Spring Security | JWT authentication |
| Spring Data JPA | Request logging to PostgreSQL |
| Redis (Lua scripts) | Token-bucket state storage |
| Thymeleaf + Chart.js | Dashboard UI |
| Maven | Build tool |
| Docker | Containerised Redis |

---

## 📦 Prerequisites

- **Java 21** (or 17)
- **Maven** (or use the included Maven wrapper `./mvnw`)
- **Docker** (for Redis)
- **PostgreSQL** – local or cloud (e.g., Neon)

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/your-username/api-gateway.git
cd api-gateway
```

### 2. Start Redis (Docker)

If the container doesn’t exist:

```bash
sudo docker run -d --name redis-gateway -p 6379:6379 redis:7
```

If it already exists (e.g., after a reboot), just start it:

```bash
sudo docker start redis-gateway
```

Verify Redis is running:

```bash
sudo docker ps | grep redis
```

### 3. Configure PostgreSQL

Set the following environment variables (or update `application.properties` directly):

```bash
export DB_URL='jdbc:postgresql://your-host:5432/your-db?sslmode=require'
export DB_USER='your-username'
export DB_PWD='your-password'
```

> **Note** – If you use a local PostgreSQL instance, omit `sslmode=require`.

### 4. Configure JWT Secret

Generate a Base64 secret key:

```bash
openssl rand -base64 32
```

Add it to `application.properties`:

```properties
jwt.secret=YOUR_BASE64_SECRET
jwt.expiration.ms=3600000
```

### 5. Run the application

```bash
./mvnw spring-boot:run
```

Or if you have Maven installed globally:

```bash
mvn spring-boot:run
```

The gateway will start on:

```text
http://localhost:8080
```

---

## 📖 How to Use

### Obtain a JWT token

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"anything"}'
```

Example response:

```json
{
  "token":"eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### Call a protected endpoint

```bash
curl -X GET http://localhost:8080/api/hello \
  -H "Authorization: Bearer <your-token>"
```

---

### Test the rate limiter

Send many requests to the audit endpoint:

```bash
for i in {1..150}; do
  curl -X POST http://localhost:8080/api/rate-limit-demo/audit/request \
    -H "Authorization: Bearer <your-token>" \
    -H "Content-Type: application/json" \
    -d '{"message":"test"}'
done
```

After the bucket is exhausted, the API returns:

```text
HTTP 429 Too Many Requests
```

---

### View the analytics dashboard

Open:

```text
http://localhost:8080/dashboard
```

---

## 🔌 API Endpoints

| Method | Endpoint | Description | Rate Limited? |
|--------|----------|-------------|----------------|
| POST | `/auth/login` | Login → returns JWT | ❌ |
| GET | `/api/hello` | Test endpoint | ✅ |
| POST | `/api/rate-limit-demo/audit/request` | Audit request | ✅ |
| POST | `/api/rate-limit-demo/audit/reset` | Audit reset | ✅ |
| GET | `/api/rate-limit-demo/config` | Show limiter configuration | ❌ |
| GET | `/api/rate-limit-demo/state` | Show token bucket state | ❌ |
| POST | `/api/rate-limit-demo/reset` | Reset token bucket | ❌ |
| GET | `/api/analytics/summary` | Analytics summary | ❌ |
| GET | `/dashboard` | Dashboard UI | ❌ |

> Rate limiting applies only to selected audit endpoints. Dashboard and analytics APIs remain open so monitoring always works.

---

## 🧪 Validation

Run tests:

```bash
./mvnw test
```

Start application:

```bash
export DB_URL='...'
export DB_USER='...'
export DB_PWD='...'

./mvnw spring-boot:run
```

Manual validation flow:

1. Login and get JWT token.
2. Call audit endpoint repeatedly.
3. Receive `429 Too Many Requests`.
4. Refresh dashboard and view logs/analytics.

---

## 📁 Project Structure

```text
api-gateway/
├── src/main/java/com/gateway/api_gateway/
│   ├── config/
│   │   ├── RedisConfig.java
│   │   ├── SecurityConfig.java
│   │   └── WebConfig.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── AnalyticsController.java
│   │   ├── DashboardController.java
│   │   └── RateLimitDemoController.java
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   └── LoginResponse.java
│   ├── filter/
│   │   └── JwtAuthenticationFilter.java
│   ├── interceptor/
│   │   ├── LoggingInterceptor.java
│   │   └── RateLimitingInterceptor.java
│   ├── model/
│   │   └── ApiRequestLog.java
│   ├── repository/
│   │   └── ApiRequestLogRepository.java
│   └── service/
│       ├── JwtService.java
│       ├── RateLimiterService.java
│       ├── AnalyticsService.java
│       └── LoggingService.java
├── src/main/resources/
│   ├── application.properties
│   └── templates/
│       └── dashboard.html
├── pom.xml
└── README.md
```

---

## 📸 Screenshots

### Database API Log

![Dashboard](docs/images/1.png)

*Dashboard showing request analytics.*

---

### Dashboard

![Rate limiter test](docs/images/2.png)
*Dashboard showing request analytics.*


---

### Rate Limiter Test

![Token bucket state](docs/images/3.png)

*Sending requests until rate limit is reached.*


---

### Token Bucket State

![Analytics API response](docs/images/4.png)

*Inspecting Redis token bucket values.*

---

## 🔒 How Rate Limiting Works

This project uses the **Token Bucket Algorithm**.

Each user/IP gets a bucket with limited tokens.

- Every request consumes 1 token.
- Tokens refill gradually over time.
- If no tokens remain → request is rejected with `429`.

Redis stores the token state and Lua scripts ensure atomic operations.

---

## 🤝 Contributing

Contributions are welcome.

1. Fork the repository
2. Create a feature branch
3. Commit changes
4. Open a Pull Request

---

## 🙏 Acknowledgements

- Spring Boot
- Spring Security
- Redis
- PostgreSQL
- JJWT
- Chart.js

---

## 📧 Contact

**Maintainer:** Aniket Yelameli  
**GitHub:** https://github.com/Aniket-The-TechWhiz