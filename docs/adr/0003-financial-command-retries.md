# ADR 0003: Synchronous financial commands and durable retry identity

- Status: Accepted
- Date: 2026-11-14

## Context

Transfer callers need a definitive result when available, but network failures can occur after Ledger commits. Kafka cannot make a debit and credit atomic across databases.

## Decision

Transfer calls Ledger synchronously over REST using a dedicated Keycloak client-credentials identity. Persist a unique client idempotency key and stable transfer operation ID before posting. Ledger enforces unique operation IDs and request hashes. Ambiguous failures remain `PENDING` and are reconciled by retrying the same ID. After a successful posting, Transfer commits its state and outbox event together; Kafka distributes the event and Notification Service durably deduplicates by event ID.

## Consequences

The transfer HTTP request may return `202 PENDING` and require a retry or status read. Outbox publication is at least once, so consumers must keep durable deduplication. Loan event integration and a background transfer reconciliation job remain unimplemented.
