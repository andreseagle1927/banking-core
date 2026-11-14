# ADR 0007: Hibernate persistence with Flyway schema ownership

- Status: Accepted
- Date: 2026-11-14

## Context

Persistence should be expressed through service-local ORM mappings and repositories while preserving explicit, reviewable database history. The Ledger also requires reliable database row locks and append-only accounting history.

## Decision

Use Spring Data JPA with Hibernate in each of the six services. Keep JPA entities and repositories service-local and in infrastructure packages; map them to framework-independent domain types at adapter boundaries. Flyway owns schema creation and evolution, and Hibernate runs with `ddl-auto: validate` and Open Session in View disabled. Prefer ORM queries for normal persistence; retain narrow PostgreSQL upserts where `ON CONFLICT` protects idempotent account provisioning or event-inbox deduplication. Ledger balance updates use JPA pessimistic locks in deterministic account order, and the database trigger continues to prohibit journal-entry mutation.

Customer, Wallet, and Loan receive initial schemas and mappings, but this decision does not implement their business workflows or public API behavior.

## Consequences

Schema changes remain versioned and reproducible. Persistence technology remains outside domain models. A small amount of PostgreSQL-specific behavior remains explicit at the persistence boundary where it enforces concurrency or deduplication invariants.
