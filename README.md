Distributed Payment Processing Simulator
A production-style distributed payment processing system built with Java 17, Spring Boot, JPA/Hibernate, PostgreSQL, Redis, Docker, and Maven.
This project simulates real-world payment backend architecture including:
✅ Idempotent transaction processing
✅ Optimistic locking for concurrency control
✅ ACID-compliant database transactions
✅ Redis-backed distributed caching
✅ Ledger-based accounting
✅ Dockerized environment
✅ CI pipeline with coverage enforcement (85%+)
🧠 Architecture Overview
Client
   |
   v
Spring Boot REST API (MVC Layer)
   |
   v
Service Layer (Transactional + Business Logic)
   |
   |---- PostgreSQL (ACID, JPA/Hibernate ORM)
   |
   |---- Redis (Idempotency + Caching)
⚙️ Tech Stack
Layer	Technology
Language	Java 17
Framework	Spring Boot 3
Web	Spring MVC
ORM	JPA / Hibernate
Database	PostgreSQL
Caching	Redis
Migration	Flyway
Testing	JUnit 5, Mockito
Build	Maven
Containerization	Docker
CI	GitHub Actions
🔥 Core Engineering Concepts Demonstrated
1️⃣ Idempotency (Duplicate Payment Protection)
Uses Idempotency-Key header
Redis stores request hash + payment ID
Prevents duplicate transaction processing
Detects conflicting payload reuse (returns HTTP 409)
2️⃣ Optimistic Locking
@Version field in Account entity
Prevents lost updates during concurrent payments
Throws OptimisticLockingFailureException
3️⃣ ACID Transactions
@Transactional service layer
Debit, credit, ledger entries, and payment state change happen atomically
4️⃣ Ledger-Based Accounting
Each payment generates:
DEBIT entry for payer
CREDIT entry for payee
Ensures traceable financial consistency.
📂 Project Structure
distributed-payment-sim/
 ├── src/
 │   ├── main/
 │   │   ├── java/com/example/paymentsim/
 │   │   │   ├── controller/
 │   │   │   ├── service/
 │   │   │   ├── repository/
 │   │   │   ├── entity/
 │   │   │   ├── dto/
 │   │   │   ├── exception/
 │   │   │   └── config/
 │   │   └── resources/
 │   │       ├── application.yml
 │   │       └── db/migration/V1__init.sql
 │   └── test/
 │       └── service/
 ├── Dockerfile
 ├── docker-compose.yml
 ├── pom.xml
 └── README.md
🐳 Running with Docker (Recommended)
1️⃣ Start Infrastructure
docker compose up -d
This starts:
PostgreSQL → localhost:5432
Redis → localhost:6379
2️⃣ Run Application
mvn clean package
mvn spring-boot:run
App runs at:
http://localhost:8080
🧪 Running Tests
mvn clean test
Coverage report:
target/site/jacoco/index.html
Enforced minimum coverage: 85%
📡 API Endpoints
🏦 Create Account
POST /api/v1/accounts
{
  "ownerName": "Alice",
  "initialBalanceCents": 10000
}
💰 Deposit
POST /api/v1/accounts/{id}/deposit
💳 Create Payment
POST /api/v1/payments
Header required:
Idempotency-Key: <unique-value>
Body:
{
  "payerAccountId": "uuid",
  "payeeAccountId": "uuid",
  "amountCents": 2500
}
🔁 Idempotency Behavior
Scenario	Result
Same key + same payload	Returns same payment
Same key + different payload	409 Conflict
No key	400 Bad Request
⚡ Example Flow (cURL)
Create Accounts
curl -X POST localhost:8080/api/v1/accounts \
-H "Content-Type: application/json" \
-d '{"ownerName":"Alice","initialBalanceCents":10000}'
Create Payment
curl -X POST localhost:8080/api/v1/payments \
-H "Content-Type: application/json" \
-H "Idempotency-Key: 123-abc" \
-d '{"payerAccountId":"<UUID1>","payeeAccountId":"<UUID2>","amountCents":2500}'
Repeat with same key → returns same payment.
🛡 Concurrency Handling
Concurrent debit attempts handled by optimistic locking
Prevents double-spending
Ensures strong consistency without distributed locks
📊 Production-Style Features
Layered architecture (Controller → Service → Repository)
Clean exception handling
Structured error responses
Database indexing strategy
Flyway versioned migrations
Dockerized environment
CI pipeline ready
Cache abstraction via Spring Cache
Idempotency TTL configuration
🧱 Database Schema
Accounts
UUID primary key
balance_cents
version (optimistic locking)
Payments
idempotency_key (unique index)
payer_account_id
payee_account_id
request_hash
Ledger Entries
Direction (DEBIT / CREDIT)
Payment reference
🧠 Why This Project Matters
This simulates real backend payment architecture, including:
Exactly-once semantics
Financial consistency
Concurrency safety
Distributed system thinking
Production deployment readiness
It demonstrates backend engineering principles used in:
FinTech systems
Payment gateways
Banking APIs
High-scale transaction platforms