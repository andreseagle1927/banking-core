# Verification plan

Automated unit tests, PostgreSQL/Testcontainers schema checks, Ledger transaction tests, and controller authorization tests are included. The database tests are skipped when Docker is unavailable; CI runs them on a Docker-enabled Linux runner. A local Compose demo also exercises the authenticated transfer path through Ledger posting, Kafka delivery, notification persistence, and balance reads. Automated full-stack regression, chaos, and performance suites remain follow-up work.

## Run the automated checks

From the repository root:

```bash
mvn -B -ntp test
```

Customer and Wallet application tests verify identity-bound profile onboarding, wallet lifecycle, currency pockets, Ledger-backed balances, and account status propagation. Loan application tests cover deterministic decisions, idempotent applications and postings, amortization schedules, repayment attempts, and ownership. Transfer and Notification unit tests cover request retries and event deduplication/delivery. PostgreSQL Testcontainers checks run the real Flyway migrations and Hibernate schema validation for all six service databases. The Ledger integration suite also checks database transactions, row locking, idempotency, journal balance, loan postings, and rollback. To run only the Ledger database suite:

```bash
mvn -pl services/ledger-service -am -Dtest=LedgerPostingServicePostgresTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Run the complete reactor package and tests with `mvn -B -ntp verify`. CI uploads Surefire reports even when a test fails.

## Implemented release gates

- Transfer domain tests enforce positive amounts, two-decimal precision, and the current ISO currency policy. USD/EUR are accepted; JPY/KWD are rejected because V1 only supports two minor-unit digits.
- Transfer application tests cover the successful Ledger handoff, replay of an already-completed idempotency key, mismatched-payload conflict, and self-transfer rejection.
- Transfer recovery tests simulate a lost Ledger response and verify the same operation ID is retried while the transfer stays pending until reconciled.
- Ledger API tests reject unauthenticated requests, deny customers account-provisioning and posting roles, and allow the dedicated ledger-poster role to post.
- Ledger PostgreSQL tests assert the journal is balanced, sender/recipient balances conserve value, simultaneous $400 debits against $500 yield exactly one success, retries reuse one posting, conflicting retries fail, and insufficient funds roll back the inserted journal.
- Notification application tests assert both parties receive records once and duplicate events do not create more records.
- Customer and Wallet application tests cover onboarding persistence, wallet and pocket idempotency, ISO currency validation, authoritative Ledger balance reads, and wallet/account status changes.
- Loan tests cover automatic decisions and reason persistence, fixed-rate amortization with final-installment rounding, Ledger disbursement references, installment state, and safe retries.

The automated tests do not yet exercise all HTTP flows with Keycloak or Kafka delivery against a broker. The local Compose demo covers authenticated onboarding, wallet provisioning, funding, transfer, and Kafka-backed notification delivery; duplicate/out-of-order Kafka delivery, outbox crash recovery, and repeatable automated full-stack regression remain follow-up work.

## Checks completed on 2026-11-14

- A clean Maven reactor package completed successfully for all nine modules with `-DskipTests`; this verifies compilation and executable JAR packaging only.
- `mvn -B -ntp verify` completed successfully on 2026-11-14 with all 25 tests passing, including PostgreSQL 17.6 Testcontainers checks and Ledger authorization tests.
- Compose, Spring application YAML, OpenAPI YAML, Keycloak realm JSON, and the transfer event JSON Schema parsed successfully.
- The Customer Service JAR started standalone. `/actuator/health` returned `200` with `{"status":"UP"}`, and the customer endpoint returned `401` without a bearer token.
- The API Gateway JAR started standalone. `/actuator/health` returned `200`, and a protected route returned `401` without a bearer token.
- The full Compose stack reached healthy status for PostgreSQL, Redis, Kafka, Keycloak, all six services, and the API Gateway. `scripts/demo-transfer.sh` completed an authenticated Alice-to-Bob USD transfer: Alice was funded with 500.00, the transfer completed for 100.00, Bob's Kafka-backed notification arrived, and ledger balances were 40000 and 10000 minor units respectively. This is a local happy-path check; it does not replace automated recovery, duplicate-delivery, failure-injection, or performance tests.

## Transfer and Ledger acceptance scenarios

- Verify application use cases depend on ports and domain types rather than Hibernate, REST client, or Kafka adapter implementations (currently reviewed in code; automated ArchUnit enforcement remains follow-up work).
- Post a same-currency transfer and verify two equal journal lines, a zero-sum journal, and matching sender/recipient balance changes in one committed Ledger transaction (implemented at Ledger database boundary).
- Run simultaneous debits against one wallet where the combined amount exceeds its funds; no resulting customer balance may be negative and no partial journal may commit (implemented using concurrent Testcontainers PostgreSQL transactions).
- Replay an identical HTTP idempotency key and Kafka-style operation redelivery; verify a single journal transaction. Reuse either ID with a different payload and expect a conflict (Ledger operation retry is implemented; HTTP and Kafka delivery integration remains planned).
- Commit a Ledger posting while dropping the response or stopping Transfer before its status update; retry and reconcile to the original Ledger transaction without a second posting.
- Attempt unknown, blocked, missing, and cross-currency accounts; reject without changing either account's journal or balance.
- Verify a customer token cannot provision accounts, simulate funding, or call Ledger's transfer-posting endpoint directly; verify the Transfer Service client-credentials identity can post while operator-only APIs remain unavailable to it.
- Retry simulated funding with the same operation ID; verify the platform funding and wallet liability entries are not duplicated.
- Stop Transfer after committing a completed state/outbox row; restart it and verify the event is eventually sent. Simulate Kafka acknowledgement loss and verify consumer deduplication suppresses duplicate notifications.
- Redeliver the same transfer event and verify exactly one sender and recipient notification record exists; verify one wallet cannot query the other's notification list.
- Inject a poison transfer event and verify it retries five times, reaches the DLT, and does not block later valid records in the partition.
- Attempt to read another customer's transfer and to provide a client-selected sender identity; deny access or ignore the supplied sender identity.
- Verify journal entry updates/deletes fail and correction is represented with a reversing journal (append-only trigger exists; mutation test and correction workflow remain planned).

## Checks completed on 2026-11-15

- `mvn -B -ntp verify` passed across all nine Maven modules with 36 tests, including PostgreSQL 17.6 Testcontainers schema and Ledger posting checks.
- Compose configuration parsed successfully and all six business services, the API Gateway, PostgreSQL, Redis, Kafka, Keycloak, Mailpit, and the Keycloak bootstrap job started successfully. The bootstrap job upgraded an existing Keycloak realm in place and can be rerun idempotently.
- Because local port 8080 was already occupied, the runtime check used `GATEWAY_PORT=8180`; the gateway `/actuator/health` returned `UP`.
- `scripts/demo-transfer.sh` completed an authenticated Alice-to-Bob USD transfer through customer onboarding, wallet creation, currency provisioning, simulated funding, Ledger posting, Kafka notification, and notification reads. It returned `COMPLETED`; the Ledger reported Alice at 80000 and Bob at 20000 minor units.
- Mailpit contained sender and recipient emails after the transfer. The authenticated Wallet API returned Alice's USD balance from Ledger after the transfer and loan disbursement.
- An authenticated loan application was automatically approved, accepted, disbursed by Ledger, and returned a six-installment monthly schedule. An immediate attempt to repay future installment 1 returned HTTP 409 as designed; repayment posting and insufficient-funds behavior are covered at the application/Ledger test boundary, while the due-date scheduler is not time-accelerated in this runtime check.

## Target service and platform scenarios

- Verify independent module builds and application health checks; verify gateway routes reject missing/invalid JWTs and validate accepted Keycloak issuers.
- Verify PostgreSQL restarts preserve service state; verify unavailable Kafka/Redis do not alter ledger postings.
- Extend the full-stack suite to drive a due installment through repayment, prove retry after insufficient funds, and reconcile a lost Ledger response across a Loan restart.
- Measure the proposed 100 transfers/s, 500 balance reads/s, and p95 below 500 ms in a documented environment; these targets are not currently measured.
