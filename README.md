# Distributed Payment Simulator

A runnable Java 17 / Spring Boot demo with PostgreSQL, Redis, a REST API, and a small browser interface. It uses whole cents, database transactions, optimistic locking, and a debit/credit ledger. This is a learning project with no authentication or real payment processor integration. Use test data only.

## Start the complete app

Install Docker with Compose and start the Docker daemon. From this repository:

```sh
docker compose up --build -d
```

Open http://localhost:8080 to create two accounts, check balances, deposit test money, and send payments. PostgreSQL and Redis must pass their health checks before the app starts. The first build downloads Maven dependencies.

```sh
docker compose logs -f app       # startup and request logs
docker compose down              # stop; retain database data
```

The named `postgres-data` volume keeps accounts and payments across restarts. `docker compose down -v` deletes that demo data. Host database and Redis ports bind to localhost.

## Run with Java and Maven

Requirements: JDK 17 or later and Maven 3.6.3 or later.

```sh
docker compose up -d postgres redis
mvn clean verify
java -jar target/payment-sim.jar
```

Alternatively, use `mvn spring-boot:run`. Both paths serve the browser page and API on port 8080.

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8080` | Java application listening port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/paymentsim` | JDBC connection |
| `DB_USERNAME` | `paymentsim` | Database user |
| `DB_PASSWORD` | `paymentsim` | Database password, also used by Compose |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port for Java; host published port for Compose |
| `APP_PORT` | `8080` | Compose app port on your machine |
| `DB_PORT` | `5432` | Compose database port on your machine |

Compose supplies the container database and Redis hostnames automatically. For a host Maven process with a custom `DB_PORT`, set `DB_URL` to the matching port. Keep credentials out of git. These defaults are for local demos.

## API

All amounts are integer cents. Account and payment IDs are UUIDs.

| Method | Path | Body / behavior |
| --- | --- | --- |
| POST | `/api/v1/accounts` | `{"ownerName":"Alice","initialBalanceCents":10000}` |
| GET | `/api/v1/accounts/{id}` | Current committed balance |
| POST | `/api/v1/accounts/{id}/deposit` | `{"amountCents":500}` |
| POST | `/api/v1/payments` | Transfer; requires `Idempotency-Key` header |
| GET | `/api/v1/payments/{id}` | Payment details |
| GET | `/actuator/health` | Application and dependency health |

```sh
curl -sS http://localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"Alice","initialBalanceCents":10000}'

# Create Bob the same way, then substitute both returned account IDs.
curl -sS http://localhost:8080/api/v1/payments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: transfer-001' \
  -d '{"payerAccountId":"<alice-id>","payeeAccountId":"<bob-id>","amountCents":2500}'
```

A committed payment is the authoritative retry record in PostgreSQL. Repeating the same key and payload returns the same payment without another debit. Different payloads with the same committed key return 409. An overlapping request may receive 409; retry it with the same key. A failed transaction rolls back its payment and ledger entries, so it can be retried after fixing the cause. Keys have a maximum of 120 characters and remain reserved for the lifetime of committed records.

Balances are read directly from PostgreSQL to avoid stale cached money values. Redis is included as infrastructure for cache experiments and health monitoring; payment execution does not depend on Redis mappings. Database uniqueness and optimistic account versions protect concurrent writes. Insufficient funds, invalid amounts, malformed input, and missing headers return 400; unknown IDs return 404; conflicting writes return 409. Arithmetic overflow rolls back the transfer.

## Verify

With PostgreSQL and Redis running:

```sh
mvn clean verify
python3 scripts/smoke.py
```

Run the smoke script after starting the app. It creates demo records and checks health, the browser page, accounts, deposits, transfers, replay behavior, and resulting balances. Set `APP_URL` for another host or port.

Maven runs both `*Test` and `*IT` classes. Integration tests cover real migrations, successful transfers, duplicate requests, payload conflicts, rollback, overflow, validation, and concurrent duplicate requests. Reports are in `target/surefire-reports` and `target/site/jacoco/index.html`. `mvn verify` enforces a minimum 85% instruction coverage. GitHub Actions also launches the packaged JAR, runs the HTTP smoke script, validates Compose, and builds the Docker image.

## Troubleshooting

- Port already in use: set `APP_PORT`, `DB_PORT`, or `REDIS_PORT` before starting Compose.
- Database connection failure: inspect `docker compose ps` and `docker compose logs postgres`.
- Database password changed: an existing PostgreSQL volume retains its original credentials. Update the database credentials or deliberately recreate disposable demo data.
- `no main manifest attribute`: rebuild with `mvn clean package` and run `target/payment-sim.jar`.
- Dependency download failure: Maven and Docker need internet access for the first build.
