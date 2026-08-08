# Signal — URL Shortener Service

A production-grade URL shortener built with **Java 17 + Spring Boot 3**,
**MySQL**, and a REST API. Includes JWT authentication, custom aliases,
link expiry, click limits, password-protected links, QR code generation,
rate limiting, async click analytics (device/browser/OS/referrer/country
breakdowns), a scheduled cleanup job, and a built-in analytics dashboard
(HTML/JS + Chart.js) — no separate frontend project required.

---

## 1. Features

| Category | Details |
|---|---|
| **Shortening** | Auto-generated Base62 codes or custom aliases, optional title |
| **Link controls** | Expiry date, max-click cap (auto-deactivates), enable/disable, password protection (bcrypt) |
| **Analytics** | Per-link total clicks, unique visitors (privacy-safe), daily time series, breakdowns by device type, browser, OS, referrer, and country |
| **Dashboard** | Static HTML/JS dashboard at `/dashboard.html` — link list + Chart.js graphs |
| **Auth** | JWT access + refresh tokens, bcrypt password hashing, per-user API key |
| **Rate limiting** | Token-bucket limiter, separate buckets for link creation vs. redirect traffic |
| **QR codes** | `GET /api/urls/{code}/qrcode` returns a PNG |
| **Performance** | Caffeine cache in front of the redirect hot path; click logging is fully async so it never adds latency to a redirect |
| **Docs** | OpenAPI/Swagger UI at `/swagger-ui.html` |
| **Ops** | Flyway migrations, Actuator health/metrics, scheduled expired-link cleanup, Docker + docker-compose |
| **Legal** | Privacy Policy and Terms of Service shipped as Markdown, served via `/api/legal/*` |

---

## 2. Tech Stack

- Java 17, Spring Boot 3.3 (Web, Data JPA, Security, Validation, Cache, Actuator)
- MySQL 8 (H2 for tests)
- Flyway for schema migrations
- JWT via `jjwt`
- Caffeine for in-memory caching and rate-limit buckets
- ZXing for QR code generation
- `ua-parser` for User-Agent parsing
- springdoc-openapi for Swagger UI
- Vanilla HTML/CSS/JS + Chart.js for the dashboard (no build step needed)

---

## 3. Project Layout

```
src/main/java/com/urlshortener/
├── config/          # Security, CORS, OpenAPI, async executor, typed @ConfigurationProperties
├── controller/       # REST controllers (Auth, Url, Redirect, Analytics, Legal)
├── dto/              # Request/response payloads
├── entity/           # JPA entities: User, ShortUrl, ClickEvent
├── exception/        # Custom exceptions + GlobalExceptionHandler
├── repository/       # Spring Data JPA repositories
├── security/         # JWT service/filter, UserDetailsService, UserPrincipal
├── service/          # Business logic (shortening, analytics, rate limiting, QR, cleanup)
└── util/             # Base62 encoder, IP resolver, hashing helpers

src/main/resources/
├── application.yml, application-{dev,prod}.yml
├── db/migration/      # Flyway SQL migrations
├── legal/             # PRIVACY_POLICY.md, TERMS_OF_SERVICE.md (served via API)
└── static/            # index.html, dashboard.html, css/, js/  (the built-in dashboard)
```

---

## 4. Quick Start (Docker Compose — recommended)

```bash
cp .env.example .env
# Edit .env: set a real JWT_SECRET (openssl rand -base64 48) and DB passwords

docker compose up --build
```

The app will be available at `http://localhost:8080`. Flyway runs
migrations automatically against the `mysql` service on startup.

Open:
- `http://localhost:8080/` — shorten a URL
- `http://localhost:8080/dashboard.html` — analytics dashboard (log in first)
- `http://localhost:8080/swagger-ui.html` — interactive API docs

---

## 5. Running Locally without Docker

1. Start a MySQL 8 instance and create a database (or let
   `createDatabaseIfNotExist=true` in the JDBC URL do it for you).
2. Export the environment variables below (or edit `application.yml`
   defaults directly for a quick local run):

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=url_shortener
export DB_USERNAME=root
export DB_PASSWORD=root
export JWT_SECRET=$(openssl rand -base64 48)
export APP_BASE_URL=http://localhost:8080
```

3. Run:

```bash
mvn spring-boot:run
```

The default profile is `dev` (verbose SQL logging). Use
`-Dspring-boot.run.profiles=prod` for production-style logging.

---

## 6. Configuration Reference

All configuration lives in `application.yml` under the `app.*` namespace
and is bound to typed `@ConfigurationProperties` classes
(`AppProperties`, `JwtProperties`) — no magic strings scattered through
the codebase.

| Property | Default | Purpose |
|---|---|---|
| `app.base-url` | `http://localhost:8080` | Used to build full short URLs and QR code payloads. **Set this to your real domain in production.** |
| `app.short-code.length` | `7` | Length of randomly generated codes (random strategy only) |
| `app.short-code.strategy` | `base62` | `base62` (counter-based, no collisions) or `random` |
| `app.short-code.alphabet` | ambiguous chars removed | Character set used for generated codes |
| `app.security.jwt.secret` | — | **Must** be overridden via `JWT_SECRET` env var in production (min 256 bits) |
| `app.security.jwt.access-token-expiration-ms` | `3600000` (1h) | Access token lifetime |
| `app.security.jwt.refresh-token-expiration-ms` | `604800000` (7d) | Refresh token lifetime |
| `app.rate-limit.enabled` | `true` | Toggle rate limiting globally |
| `app.rate-limit.creation.*` | 30 links / 60s | Token bucket for `POST /api/urls` |
| `app.rate-limit.redirect.*` | 300 hits / 60s | Token bucket for `GET /r/{code}` (per IP) |
| `app.url-validation.blocked-domains` | example placeholders | Domains rejected at creation time |
| `app.url-validation.max-url-length` | `2048` | Max accepted destination URL length |
| `app.qr-code.size-px` | `300` | QR code image size |
| `app.cors.allowed-origins` | `localhost:8080,3000` | Comma-separated CORS allow-list |

Environment variables map onto these via Spring's relaxed binding (see
`application.yml` for the full `${VAR:default}` list) — the ones you'll
actually set in production are: `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `APP_BASE_URL`,
`CORS_ALLOWED_ORIGINS`, `SERVER_PORT`.

---

## 7. API Overview

Full interactive docs at `/swagger-ui.html`. Summary:

### Auth (public)
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Create an account |
| POST | `/api/auth/login` | Get access + refresh tokens |
| POST | `/api/auth/refresh` | Exchange a refresh token for a new access token |

### URLs (create is public; management requires a Bearer token)
| Method | Path | Description |
|---|---|---|
| POST | `/api/urls` | Shorten a URL. Works anonymously (IP rate-limited) or authenticated (saved to your account) |
| GET | `/api/urls` | List your links (paginated) — **auth required** |
| GET | `/api/urls/{code}` | Get one of your links — **auth required** |
| PATCH | `/api/urls/{code}` | Update title/expiry/active/password/click-limit — **auth required** |
| DELETE | `/api/urls/{code}` | Delete a link — **auth required** |
| GET | `/api/urls/{code}/qrcode` | PNG QR code — public |

### Redirect (public)
| Method | Path | Description |
|---|---|---|
| GET | `/r/{code}` | 302 redirect to the destination. Returns `401` with `X-Password-Required: true` if the link is password-protected |
| POST | `/r/{code}/unlock` | Submit `{ "password": "..." }` to redirect through a protected link |

### Analytics (auth required, owner-only)
| Method | Path | Description |
|---|---|---|
| GET | `/api/analytics/{code}?days=30` | Totals, unique visitors, daily time series, and breakdowns by referrer/device/browser/OS/country |

### Legal (public)
| Method | Path |
|---|---|
| GET | `/api/legal/privacy-policy` |
| GET | `/api/legal/terms` |

### Example: create a short link

```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{
        "originalUrl": "https://example.com/some/very/long/path",
        "customAlias": "launch-2026",
        "expiresAt": "2026-12-31T23:59:59",
        "maxClicks": 500
      }'
```

---

## 8. How the Redirect Hot Path Stays Fast

`GET /r/{code}` is the most frequently hit endpoint and the one users
actually wait on, so it's optimized deliberately:

1. **Cached lookup** — `UrlShortenerService.findActiveByShortCode` is
   `@Cacheable` (Caffeine, 10 min TTL), so repeat hits on a popular link
   skip the database entirely.
2. **Async click logging** — `ClickTrackingService.recordClickAsync` runs
   on a dedicated thread pool (`AsyncConfig`) and is fired *after* the
   redirect decision is made, so User-Agent parsing and the analytics
   INSERT never block the 302 response.
3. **Row-level locking only where needed** — click-count increments use a
   `SELECT ... FOR UPDATE` (`findByIdForUpdate`) confined to a small
   transaction, avoiding lost updates under concurrent clicks without
   locking the whole table.

---

## 9. Privacy & Security Notes

- Raw IP addresses are **never persisted**. Only a salted SHA-256 hash
  (`ipHash`) is stored, sufficient for rough unique-visitor counts. See
  `PRIVACY_POLICY.md` / `GET /api/legal/privacy-policy` for the full
  policy — keep it in sync with `ClickTrackingService` if you change what
  is collected.
- Account and link-access passwords are bcrypt-hashed (work factor 12).
- JWT access tokens are short-lived (1h default); refresh tokens are
  longer-lived (7d default) and only ever exchanged over the dedicated
  `/api/auth/refresh` endpoint.
- Ownership checks on every management/analytics endpoint prevent one
  account from reading or modifying another account's links.
- `app.url-validation.blocked-domains` gives you a simple, configurable
  domain blocklist; wire it up to a real threat-intel feed for production
  use if you need stronger protection against phishing/malware links.

---

## 10. Testing

```bash
mvn test
```

Tests run against an in-memory H2 database (`application-test.yml`), so
no external MySQL instance is required. Includes:
- Unit tests for the Base62 encoder, hashing utilities, URL validation,
  and short-code collision retry logic (Mockito).
- `@DataJpaTest` repository tests (expiry queries, click-count updates).
- A Spring context smoke test (`contextLoads`) that exercises the full
  security/JPA/scheduling wiring.

---

## 11. Scaling Notes (beyond a single instance)

This project ships as a solid single-instance deployment. To scale
horizontally:

- **Rate limiter**: `RateLimiterService` uses an in-memory Caffeine
  bucket per instance. Swap it for a Redis-backed limiter (e.g.
  Bucket4j + Redis) so limits are shared across nodes — the
  `tryConsumeForCreation` / `tryConsumeForRedirect` method signatures are
  designed to be a drop-in replacement point.
- **Cache**: same idea — replace the Caffeine `CacheManager` with a Redis
  `CacheManager` for a shared hot-path cache across nodes.
- **DB**: add read replicas for analytics queries (`AnalyticsService`)
  once click volume grows, since those are read-heavy and separable from
  the write-heavy redirect path.

---

## 12. License

This is a reference/starter implementation provided as-is for you to
adapt. Add your own license file before publishing or deploying publicly.
