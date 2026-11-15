# Core Banking System — Architecture v1.0

## Product and operating assumptions

The prototype is a closed-loop digital banking system for up to 100,000 registered users. A customer has one wallet with multiple currency pockets. V1 transfers are same-currency only; FX is out of scope. Platform funding is simulated and controlled. Customer onboarding, wallet lifecycle, transfers, rule-based loan origination, monthly repayment, and notification delivery are implemented prototype workflows. A single local PostgreSQL server hosts logical databases; service ownership and migration histories remain separate.

The initial illustrative performance goals (100 transfers/s, 500 balance reads/s, p95 transfer latency below 500 ms) are test hypotheses, not measured capacity guarantees. Local Compose is for development/integration, not availability guarantees.

## Service boundaries and state ownership

| Component | Owns | Runtime state today |
|---|---|---|
| Customer | Profile and onboarding keyed to the authenticated identity | PostgreSQL profile; local prototype activates self-onboarded customers without real KYC |
| Wallet | One wallet per customer, wallet status, currency pockets | PostgreSQL ownership/pocket state; reads balances from Ledger |
| Transfer | Request limits, idempotency, transfer status | PostgreSQL and Flyway; same-currency workflow implemented |
| Ledger | Accounts, journals, materialized balances, atomic postings | PostgreSQL and Flyway; simulated funding and transfers implemented |
| Loan | Applications, deterministic decisions, contracts, installments, repayment attempts | PostgreSQL lifecycle state; Ledger owns all monetary postings; daily due-installment scheduler |
| Notification | Delivery and notification history | Kafka consumer, durable event deduplication, PostgreSQL in-app records and retryable email queue; local email is captured by Mailpit |
| API Gateway | Routing and JWT authentication | Spring Cloud Gateway; stateless |
| Keycloak | Identities, credentials, OAuth2/OIDC tokens | Local development realm with Alice and Bob |

Customer, Wallet, Transfer, Ledger, Loan, and Notification own logical databases and users. All six services use Hibernate/Spring Data JPA mappings validated against Flyway-owned schemas. No JPA entities, repositories, or business models cross service boundaries. `platform-security` shares only the HTTP security filter configuration.

The implemented workflows follow hexagonal boundaries: HTTP controllers, scheduled jobs, and the Kafka listener are inbound adapters; application services coordinate use cases through domain types and outbound ports; Hibernate/JPA, Keycloak-protected REST clients, SMTP, and Kafka/outbox adapters implement those ports. Ledger alone owns financial postings; Loan owns loan contracts and repayment schedules, and Wallet owns wallet and currency-pocket lifecycle state.

## Topology and command path

```mermaid
flowchart LR
  Client -->|HTTPS / REST + JWT| Gateway
  Gateway --> Customer
  Gateway --> Wallet
  Gateway --> Transfer
  Gateway --> Loan
  Transfer -->|sync REST, stable operationId| Ledger
  Ledger --> LedgerDB[(ledger_db)]
  Transfer --> TransferDB[(transfer_db)]
  Gateway -. JWT validation .-> Keycloak
  Customer -. JWT validation .-> Keycloak
  Wallet -. JWT validation .-> Keycloak
  Transfer -. JWT validation .-> Keycloak
  Ledger -. JWT validation .-> Keycloak
  Loan -. JWT validation .-> Keycloak
  Notification -. JWT validation .-> Keycloak
  Transfer -. transfers.events.v1 .-> Kafka
  Kafka -. transfers.events.v1 .-> Notification
  Redis[(Redis)] -. future cache and rate limits .-> Gateway
```

The implemented transfer request is `POST /api/v1/transfers` with `Authorization: Bearer <JWT>`, `Idempotency-Key`, and `{recipientWalletId, amount, currency}`. The sender is derived from JWT `sub`, never accepted from the client. In this prototype the local Keycloak subject UUID is used directly as the wallet owner UUID. Production code must resolve customer ownership through the Wallet/Customer domain and enforce transfer limits and authorization there.

Transfer persists a `PENDING` row under a unique idempotency key before calling Ledger synchronously. Its stable transfer UUID is the Ledger operation ID. Transfer obtains a Keycloak client-credentials token as `transfer-service` for the internal Ledger call; Ledger grants posting permission only to this client identity. If a call times out, Transfer retains `PENDING`; a retry repeats the same posting request. Ledger has a unique operation ID plus a request hash, so an identical retry returns the committed journal ID and a mismatched retry conflicts. A definitive insufficient-funds or invalid-account response becomes `REJECTED`. On success, Transfer commits the `COMPLETED` state and a `TransferCompleted` outbox event in one Transfer database transaction. A publisher sends outbox rows to Kafka and marks them published after broker acknowledgement; a crash after send can produce redelivery.

Notification Service consumes `transfers.events.v1`, inserts the event ID into its inbox and creates sender and recipient notification records in one Notification database transaction. Duplicate Kafka delivery is ignored by the durable processed-event primary key. Processing failures retry five times with a one-second backoff before the record is published to `transfers.events.v1.DLT` for operator review/replay. Users can read their notifications through `GET /api/v1/notifications/me`, with wallet identity derived from the token subject.

Ledger locks both account balance rows in UUID order using PostgreSQL `FOR UPDATE`, checks the sender's spendable balance, then writes both journal entries and updates both balance rows inside one PostgreSQL transaction. V1 supports ISO currencies with exactly two minor-unit digits; values are accepted as decimal strings and stored in integer minor units. Cross-currency debits and credits are rejected by the service boundary; no FX conversion exists.

Customer wallet balances are liabilities: a transfer debits the sender's liability account and credits the recipient's liability account. Simulated funding debits a platform funding account and credits the wallet liability. This records a balanced journal but does not represent settled external cash or capital. Journal entries cannot be updated or deleted; corrections require compensating journals (correction workflow not implemented yet).

## API and event contracts

Gateway routes `/api/v1/customers/**`, `/api/v1/wallets/**`, `/api/v1/transfers/**`, and `/api/v1/loans/**`. Ledger endpoints are internal and are not routed by the gateway. The local Compose file publishes Ledger's port only for development provisioning and balance inspection; production deployments must keep it private and use dedicated service credentials.

Initial Ledger endpoints are `POST /internal/ledger/accounts`, `POST /internal/ledger/funding`, `POST /internal/ledger/transfers`, and `GET /internal/ledger/accounts/{walletId}/balances/{currency}`. Account provisioning and simulated funding require the local `platform-operator` realm role. Balance reads are restricted to the wallet owner or an operator. Transfer posting requires the `ledger-poster` realm role, assigned only to the `transfer-service` Keycloak service account; customer tokens cannot invoke the posting endpoint directly. The local service secret is a development credential and must be replaced by a managed secret in deployment.

Kafka carries `TransferCompleted` on `transfers.events.v1`; its envelope and payload are specified in [the versioned schema](../../contracts/schemas/transfer-completed-v1.schema.json). Transfer uses an outbox and Notification uses a durable processed-event inbox. Delivery is at least once; the notification inbox makes replay safe. Kafka is not part of the atomic posting path. Loan event contracts and producers remain future work.

## Data and runtime

PostgreSQL owns authoritative records. Redis is reserved for non-authoritative caching/rate limiting. Kafka is for asynchronous integration. Keycloak validates OIDC tokens at the gateway and in each service. Compose uses persistent development volumes and a PostgreSQL readiness check. Local credentials, HTTP, single-node Kafka, and `start-dev` Keycloak are deliberately non-production settings.

## Deferred production work

Prototype limits and remaining work: Customer onboarding activates a profile without real KYC or customer-status review; transfer limits and richer eligibility checks are not implemented; email is captured locally and SMS/push are out of scope; Loan events, financial reconciliation, and audit workflows remain future work. Production secret rotation, TLS/mTLS, MFA policy, rate limiting, observability/alerts, backup/restore automation, and load/resilience testing are not configured. The system and local demo do not provide production banking, custody, settlement, or lending controls.
