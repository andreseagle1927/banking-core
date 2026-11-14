# ADR 0002: Ledger as the authoritative money system

- Status: Accepted
- Date: 2026-11-14

## Context

Transfers, simulated funding, and later loan disbursements/repayments must preserve financial integrity under concurrency and retries.

## Decision

Only Ledger posts money. Represent customer wallet balances as liabilities and use balanced double-entry journal rows. Transfer postings lock account balance rows in stable UUID order, validate funds, append both entries, and update the materialized balance rows in one PostgreSQL transaction. Journal entries are append-only; corrections use reversing entries. Use integer minor units in Ledger and decimal API amounts. V1 accepts only currencies with two minor-unit digits and supports same-currency operations only. Redis is never authoritative for balances.

In the local prototype, account provisioning and simulated funding require a dedicated Keycloak operator role, while transfer posting requires the `ledger-poster` role assigned to the `transfer-service` Keycloak service account. Balance reads are scoped to the wallet owner or operator. The development client secret must be replaced by a managed secret, and Ledger must remain private in deployed environments.

## Consequences

The Ledger is the consistency boundary and can become a contention point requiring measurement. Simulated funding produces balanced internal accounting but proves no external cash settlement. Reversal and reconciliation operations remain to be implemented.
