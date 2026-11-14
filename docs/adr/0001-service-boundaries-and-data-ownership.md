# ADR 0001: Service boundaries and data ownership

- Status: Accepted
- Date: 2026-11-14

## Context

The prototype includes customer, wallet, transfer, ledger, loan, and notification capabilities. Combining all data in one deployable would undermine the intended independent service ownership; using a database server/container per service locally would add avoidable development overhead.

## Decision

Use six business services, one stateless API gateway, and Keycloak. Each service owns its schema, database user, and migrations. Local Compose runs one PostgreSQL server with one logical database and role per service. Each service may define its own Hibernate entities and Spring Data repositories; do not share business entities, JPA models, or repositories across services. Share published API/event contracts and minimal cross-cutting configuration only. Customer, Wallet, and Loan have initial persistence schemas, while their business workflows remain skeletons.

## Consequences

Services can be built and deployed as separate applications while local development remains lightweight. Cross-service queries require APIs or events. Database-per-service infrastructure and production deployment topology remain environment decisions.
