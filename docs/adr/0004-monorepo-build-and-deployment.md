# ADR 0004: Maven monorepo with independently deployable applications

- Status: Accepted
- Date: 2026-11-14

## Context

The project needs coordinated dependency versions and a single local developer workflow, while retaining independent service release/deployment units.

## Decision

Use a root Maven aggregator with one module per service/gateway and a small cross-cutting security module. Pin Java 21, Spring Boot 3.5.x, and the compatible Spring Cloud 2025.0.x train. Build individual deployables with Maven reactor selection (`-pl ... -am`) and a service-specific Docker build argument. Compose provides local infrastructure only; it is not a production orchestration choice.

## Consequences

Dependency convergence is easy to review centrally, while each application still yields an executable JAR/image. Build-time coupling to the shared security module remains; avoid growing it into a shared business library.
