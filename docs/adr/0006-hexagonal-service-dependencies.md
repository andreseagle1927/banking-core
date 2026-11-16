# ADR 0006: Hexagonal dependencies for business workflows

- Status: Accepted
- Date: 2026-11-14

## Context

Business rules for posting money and issuing notifications must remain independent from HTTP, SQL, and Kafka details. The initial workflow needs synchronous Ledger communication plus durable persistence and event delivery.

## Decision

Organize each service around inbound adapters, application use cases, domain types, and outbound ports. Keep controllers focused on HTTP translation, each use case focused on one application operation, and each infrastructure adapter focused on one external concern. Hibernate/JPA repositories, REST clients, and Kafka/outbox implementations live in infrastructure packages behind application-owned ports. Keep ports narrow to the consuming use case, and require every adapter implementation to preserve that port's behavior contract. Application and domain rules depend on ports, not on infrastructure implementations; do not share business persistence models across services.

## Consequences

Customer, Wallet, Transfer, Ledger, Loan, and Notification use this dependency direction for the implemented workflows. This applies SOLID through single-purpose classes and services, extension through ports, substitutable adapters, consumer-focused interfaces, and dependency inversion. Framework annotations stay in service/application and infrastructure code; core domain values use JDK types only.
