# Shop API

![CI](https://github.com/KatiaBoutarfa/ShopApp/actions/workflows/ci.yml/badge.svg)

REST API for a small e-commerce platform: users, products and orders, secured with JWT access tokens and rotating refresh tokens.

The project is built in phases. It is currently a **modular monolith** with Docker, Redis caching and continuous integration. Later phases split it into microservices (see the [roadmap](#roadmap)).

## Tech stack

- Java 21, Spring Boot 4 (Web MVC, Data JPA, Security, Validation)
- MySQL 8.4 with Flyway migrations
- Redis for caching
- JWT (jjwt) for access tokens, opaque hashed refresh tokens stored in the database
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, Testcontainers (real MySQL and Redis in tests)
- Docker and Docker Compose
- GitHub Actions for continuous integration, Dependabot for dependency updates

## Features

- Registration and login, access token in the response body, refresh token in an `HttpOnly` cookie
- Refresh token rotation with reuse detection; logout revokes the token
- Role and permission based authorization (`USER`, `ADMIN`)
- Product catalog with search, pagination and soft delete
- Orders with price snapshot per line, stock control and cancellation
- Optimistic locking on stock to prevent selling the last item twice
- Product lookup by id cached in Redis for 10 minutes, evicted on product update, deletion and stock changes (order creation and cancellation)
- The application keeps working, only slower, if Redis is unavailable
- Uniform JSON error format

## Quick start (Docker Compose)

This is the recommended way to run the project. It starts the application, MySQL and Redis.

Prerequisite: Docker Desktop (or Docker Engine) running.

1. Create your environment file from the template:

   ```bash
   cp .env.example .env
   ```

   On Windows PowerShell: `Copy-Item .env.example .env`

2. Edit `.env` and set the three values:

   | Variable | Description |
   |---|---|
   | `DB_PASSWORD` | Password of the application database user |
   | `DB_ROOT_PASSWORD` | Password of the MySQL root user (container only) |
   | `JWT_SECRET` | Base64 secret of at least 32 bytes. Generate one with `openssl rand -base64 32` |

3. Build and start everything:

   ```bash
   docker compose up --build
   ```

4. Open Swagger UI: http://localhost:8080/swagger-ui.html

Useful commands:

| Command | Effect |
|---|---|
| `docker compose logs -f app` | Follow the application logs |
| `docker compose down` | Stop and remove the containers (database data is kept in a volume) |
| `docker compose down -v` | Also delete the volume, which erases the database |

The `.env` file contains secrets and is ignored by Git. Never commit it.

## Configuration reference

The application reads its settings from environment variables:

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/shop_db` |
| `DB_USERNAME` | Database user | `shop_user` |
| `DB_PASSWORD` | Database password | none, required |
| `JWT_SECRET` | Base64 secret, at least 32 bytes | none, required |
| `REDIS_HOST` | Redis host | `localhost` |
| `APP_COOKIE_SECURE` | `Secure` flag of the refresh cookie | `true` |
| `SPRING_PROFILES_ACTIVE` | Use `dev` for local development | none |

`APP_COOKIE_SECURE=false` is only for development over plain HTTP (Docker Compose sets it for you). In production the application must run behind HTTPS with the flag set to `true`.

## Running without Docker (local development)

Requires JDK 21, MySQL 8.x and Redis running on `localhost`.

Create the database and user:

```sql
CREATE DATABASE shop_db CHARACTER SET utf8mb4;
CREATE USER 'shop_user'@'localhost' IDENTIFIED BY 'choose_a_strong_password';
GRANT ALL PRIVILEGES ON shop_db.* TO 'shop_user'@'localhost';
```

Create `src/main/resources/application-dev.properties` (ignored by Git):

```properties
app.cookie.secure=false
logging.level.org.springframework.security=DEBUG
```

Set `DB_PASSWORD`, `JWT_SECRET` and `SPRING_PROFILES_ACTIVE=dev`, then run:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux and macOS use `./mvnw spring-boot:run`. Flyway creates the schema on startup.

## Tests

```powershell
.\mvnw.cmd clean test
```

On Linux and macOS use `./mvnw clean test`.

The suite covers unit tests, slice tests (`@WebMvcTest`, `@DataJpaTest`) and integration tests against real MySQL and Redis containers started by Testcontainers, including a concurrency test for the last item in stock and a cache eviction test. Docker must be running.

The same suite runs on every push and pull request through GitHub Actions (`.github/workflows/ci.yml`), which also checks that the Docker image builds.

## API overview

### Auth (public)

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Create an account |
| POST | `/api/auth/login` | Returns an access token and sets the refresh cookie |
| POST | `/api/auth/refresh` | Rotates the refresh token, returns a new access token |
| POST | `/api/auth/logout` | Revokes the refresh token |

### Products

| Method | Path | Access |
|---|---|---|
| GET | `/api/products?keyword=&page=&size=` | Public |
| GET | `/api/products/{id}` | Public |
| POST | `/api/products` | `PRODUCT_WRITE` |
| PUT | `/api/products/{id}` | `PRODUCT_EDIT` |
| DELETE | `/api/products/{id}` | `PRODUCT_DELETE` (soft delete) |

### Orders

| Method | Path | Access |
|---|---|---|
| POST | `/api/orders` | `ORDER_WRITE` |
| GET | `/api/orders` | `ORDER_READ`, own orders only |
| GET | `/api/orders/{id}` | Owner or admin |
| PATCH | `/api/orders/{id}/cancel` | Owner, pending orders only |

Protected endpoints expect `Authorization: Bearer <accessToken>`.

## Database schema

- `user` (id, email unique, password_hash, full_name, created_at, role)
- `product` (id, name unique, description, price, stock, active, created_at, version)
- `orders` (id, user_id, total, status, created_at, version)
- `order_items` (id, order_id, product_id, quantity, unit_price)
- `refresh_token` (id, token_hash unique, user_id, expires_at, revoked)

Migrations live in `src/main/resources/db/migration`.

## Design notes

- Packages are organized by domain (`auth`, `user`, `product`, `order`) to ease a later split into services.
- Prices are `DECIMAL(19,2)` / `BigDecimal`; each order line stores the unit price at purchase time.
- Timestamps are stored in UTC (`Instant`, `DATETIME(6)`).
- Refresh tokens are stored as SHA-256 hashes; presenting a revoked token revokes all sessions of that user.
- Caching follows the cache-aside pattern. Entries are evicted after the transaction commits, so a concurrent read cannot repopulate the cache with stale data. Cache errors are logged and ignored.
- The Docker image is built in two stages (JDK to build, JRE to run) and runs as a non-root user. Secrets are injected at runtime, never baked into the image.
- CI workflow permissions are limited to read-only repository access.

## Roadmap

1. **Phase 1, modular monolith (done):** REST API, authentication and authorization, products, orders, database migrations, unit, slice and integration tests with Testcontainers.
2. **Phase 2, containerization and CI (done):** Docker Compose, Redis caching, GitHub Actions CI.
3. **Phase 3, microservices:** split into services with an API gateway.
4. **Phase 4, messaging:** RabbitMQ and Kafka.
5. **Phase 5, advanced processing:** Spring Batch reports and WebFlux live stats.
