# Core Banking Monorepo

Java 21 / Spring Boot monorepo for a closed-loop digital banking prototype. Each business service is a separate Maven module and deployable container; data ownership is kept separate even though local development uses one PostgreSQL server.

## What is implemented

- Seven runnable Spring applications across eight Maven modules: the gateway, six business services, and a shared security library.
- Keycloak JWT validation at the gateway and each service.
- A working same-currency transfer path: authenticated request → idempotent Transfer record → atomic Ledger posting → transactional transfer outbox → Kafka event → deduplicated notification records.
- Simulated platform funding for local demos. Registration does not create money.
- PostgreSQL/Flyway migrations and Hibernate validation for all six service databases. Append-only journal entry protection is enforced by a database trigger.
- Local Compose infrastructure for PostgreSQL, Redis, Kafka, Keycloak, and the service containers.

Customer, Wallet, and Loan remain authenticated API skeletons, with initial database schemas and Hibernate mappings available for future workflows. Lending, reconciliation, external notification delivery, observability, and performance targets remain future implementation work. Redis is provisioned but unused; Kafka carries post-commit transfer events and is not in the atomic money path.

This is a development prototype, not a production-ready bank. Local Keycloak passwords, database passwords, plaintext transport, simulated funding, and lack of real lending/compliance controls are strictly for local development.

## Start locally

Prerequisites: Java 21, Maven 3.9+, Docker Engine, and Docker Compose v2. From the repository root:

```bash
mvn -B -ntp test
mvn -B -ntp package -DskipTests
docker compose up --build
```

The automated suite includes Transfer and Notification unit tests, Ledger API authorization checks, and PostgreSQL Testcontainers database tests. See the [verification plan](docs/architecture/verification-plan.md) for covered release gates and checks still to add.

The gateway is at `http://localhost:8080`, Swagger UI at `http://localhost:8080/swagger-ui.html`, Keycloak at `http://localhost:8081`, and the Ledger's internal development port at `http://localhost:8085`. Swagger documents the current public routes and local internal Ledger routes; Customer, Wallet, and Loan are explicitly marked as scaffolds. Use the Authorize button with a local Keycloak access token to try public routes. Ledger operations are documented against its direct local port and require the appropriate role. Infrastructure credentials and ports are defined in `compose.yaml`; do not reuse them outside local development. First startup can take a few minutes while images build and Keycloak initializes. Database init scripts run only when the PostgreSQL volume is first created.

Once the stack is healthy, run `./scripts/demo-transfer.sh` to provision the demo accounts, simulate Alice's funding, make an idempotent Alice-to-Bob transfer, wait for Bob's notification, and print both Ledger balances. It requires `curl` and `python3`. The script accepts `KEYCLOAK_URL`, `GATEWAY_URL`, `LEDGER_URL`, `ALICE_PASSWORD`, `BOB_PASSWORD`, `OPERATOR_PASSWORD`, `TRANSFER_AMOUNT`, and `CURRENCY` overrides.

## Try the transfer workflow

The imported local users have fixed UUID subjects and completed demo profiles so they can stand in for wallet owner IDs during this prototype. Obtain an Alice access token (requires `curl` and `jq`):

```bash
ALICE_TOKEN=$(curl -fsS -X POST http://localhost:8081/realms/core-banking/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=banking-local&grant_type=password&username=alice&password=alice-local-password' | jq -r .access_token)
```

Ledger account provisioning and simulated funding require the separate local `operator` account. Customer tokens cannot provision accounts, fund wallets, or call Ledger's posting endpoint directly:

```bash
OPERATOR_TOKEN=$(curl -fsS -X POST http://localhost:8081/realms/core-banking/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=banking-local&grant_type=password&username=operator&password=operator-local-password' | jq -r .access_token)
```

Create USD ledger accounts and simulate a controlled $500 credit to Alice. Bob's account starts at zero:

```bash
curl -fsS -X POST http://localhost:8085/internal/ledger/accounts -H "Authorization: Bearer $OPERATOR_TOKEN" -H 'Content-Type: application/json' -d '{"walletId":"11111111-1111-4111-8111-111111111111","currency":"USD"}'
curl -fsS -X POST http://localhost:8085/internal/ledger/accounts -H "Authorization: Bearer $OPERATOR_TOKEN" -H 'Content-Type: application/json' -d '{"walletId":"22222222-2222-4222-8222-222222222222","currency":"USD"}'
curl -fsS -X POST http://localhost:8085/internal/ledger/funding -H "Authorization: Bearer $OPERATOR_TOKEN" -H 'Content-Type: application/json' -d '{"operationId":"f0000000-0000-4000-8000-000000000001","walletId":"11111111-1111-4111-8111-111111111111","amount":"500.00","currency":"USD"}'
```

Post $100 to Bob using any unique idempotency key (reusing the same key and payload returns the original transfer):

The Transfer Service derives Alice's wallet from her authenticated subject, then obtains a separate Keycloak client-credentials token for its synchronous Ledger call. The user's token is never forwarded to Ledger.

```bash
curl -i -X POST http://localhost:8080/api/v1/transfers \
  -H "Authorization: Bearer $ALICE_TOKEN" -H 'Idempotency-Key: demo-transfer-001' \
  -H 'Content-Type: application/json' \
  -d '{"recipientWalletId":"22222222-2222-4222-8222-222222222222","amount":"100.00","currency":"USD"}'
```

Expected result: `COMPLETED` with a `ledgerTransactionId`; the HTTP response is `200`. A funds rejection returns `422`; a Ledger timeout leaves the operation `PENDING` and returns `202`, so retrying the same request safely reconciles through the same Ledger operation ID. The request intentionally has no sender ID: the sender is derived from the signed token subject.

The transfer outbox publishes `TransferCompleted` to Kafka after the transfer status commits. Notification Service deduplicates the event, stores one notification for each party, and exposes the recipient's notifications at `GET /api/v1/notifications/me`:

```bash
curl -fsS -H "Authorization: Bearer $ALICE_TOKEN" http://localhost:8080/api/v1/notifications/me
```

Check balances through the internal development port:

```bash
curl -fsS -H "Authorization: Bearer $ALICE_TOKEN" http://localhost:8085/internal/ledger/accounts/11111111-1111-4111-8111-111111111111/balances/USD
BOB_TOKEN=$(curl -fsS -X POST http://localhost:8081/realms/core-banking/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=banking-local&grant_type=password&username=bob&password=bob-local-password' | jq -r .access_token)
curl -fsS -H "Authorization: Bearer $BOB_TOKEN" http://localhost:8085/internal/ledger/accounts/22222222-2222-4222-8222-222222222222/balances/USD
```

## Build one deployable module

The root POM is an aggregator and shared dependency baseline. Build an application with its required in-repo modules using `mvn -pl services/ledger-service -am package -DskipTests`, replacing the module path as needed. Each application module produces its own executable Spring Boot JAR and container image. Business entities, persistence models, and repositories are not shared between services.

## Repository map

```text
gateway/api-gateway/              Spring Cloud Gateway and JWT validation
services/{customer,wallet,transfer,ledger,loan,notification}-service/
platform/platform-security/       shared HTTP security configuration only
contracts/                         OpenAPI and versioned event schemas
infrastructure/{postgres,keycloak}/
docs/architecture/                 topology, boundaries, workflows, limitations
docs/adr/                           accepted architectural decisions
compose.yaml                       local development stack
```

See [architecture overview](docs/architecture/overview.md), the [verification plan](docs/architecture/verification-plan.md), and the [ADRs](docs/adr/).
