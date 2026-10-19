# Shop API

REST API for a small e-commerce platform: users, products and orders, secured with JWT access tokens and rotating refresh tokens.

This is **phase 1**: a modular monolith. Later phases add Docker, Redis, CI/CD and a split into microservices.

## Tech stack

- Java 21, Spring Boot 4 (Web MVC, Data JPA, Security, Validation)
- MySQL 8.4 with Flyway migrations
- JWT (jjwt) for access tokens, opaque hashed refresh tokens stored in the database
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, Testcontainers (real MySQL in tests)

## Features

- Registration and login, access token in the response body, refresh token in an `HttpOnly` cookie
- Refresh token rotation with reuse detection; logout revokes the token
- Role and permission based authorization (`USER`, `ADMIN`)
- Product catalog with search, pagination and soft delete
- Orders with price snapshot per line, stock control and cancellation
- Optimistic locking on stock to prevent selling the last item twice
- Uniform JSON error format

## Prerequisites

- JDK 21
- MySQL 8.x running locally
- Docker (only needed to run the tests, Testcontainers starts a MySQL container)

## Configuration

Secrets are never stored in the repository. Set these environment variables:

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/shop_db` |
| `DB_USERNAME` | Database user | `shop_user` |
| `DB_PASSWORD` | Database password | none, required |
| `JWT_SECRET` | Base64 secret, at least 32 bytes | none, required |
| `SPRING_PROFILES_ACTIVE` | Use `dev` locally | none |

Generate a secret: `openssl rand -base64 32`

Create the database and user:

```sql
CREATE DATABASE shop_db CHARACTER SET utf8mb4;
CREATE USER 'shop_user'@'localhost' IDENTIFIED BY 'choose_a_strong_password';
GRANT ALL PRIVILEGES ON shop_db.* TO 'shop_user'@'localhost';
```

For local development, create `src/main/resources/application-dev.properties` (ignored by Git):

```properties
app.cookie.secure=false
logging.level.org.springframework.security=DEBUG
```

## Run

```bash
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`. Flyway creates the schema on startup.

Swagger UI: http://localhost:8080/swagger-ui.html

## Tests

```bash
./mvnw clean test
```

The suite covers unit tests, slice tests (`@WebMvcTest`, `@DataJpaTest`) and integration tests against a real MySQL container, including a concurrency test for the last item in stock.

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

- `user` (id, email unique, password_hash, full_name, created_at,role)
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

## Roadmap

1. Docker, Redis cache, GitHub Actions (phase 2)
2. Split into microservices with an API gateway (phase 3)
3. RabbitMQ and Kafka messaging (phase 4)
4. Spring Batch reports and WebFlux live stats (phase 5)
