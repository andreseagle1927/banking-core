# Kubernetes deployment

The `deploy/helm/core-banking` chart deploys the API gateway and six business services as separate Kubernetes Deployments. Each has an internal ClusterIP Service. Only the gateway has an optional Ingress; Ledger has no external route.

The current SSH-only deployment on Andres's host is documented in [server-demo.md](server-demo.md). It uses the local development profile and is separate from the production profile below.

The chart has two profiles:

- `values-local.yaml` runs single-node PostgreSQL, Kafka, Keycloak and Mailpit in the cluster. PostgreSQL, Kafka and Keycloak use persistent volume claims. Redis is omitted because no service uses it.
- `values-cloud.yaml` disables in-cluster dependencies and demonstrates provider-neutral external endpoints for PostgreSQL, Kafka, OIDC, and SMTP. Replace example endpoints and image tags before deployment. It is a configuration starting point, not a production environment by itself.

The API gateway Service remains `ClusterIP` by default. On the SSH host, the production values reserve an unused stable ClusterIP for the gateway and the host Nginx reverse proxy routes the public hostname to it. Keep `gatewayIngress.enabled: false` for this topology; do not expose a NodePort.

The local profile includes development-only passwords and demo users. Do not use it on a shared cluster.

## Local Kind deployment

Prerequisites: Docker, Kind, kubectl, Helm 4, Java/Maven for building containers, and at least 6 GB of available memory.

Create the cluster and build/load each service image, then deploy:

```bash
./scripts/kind-deploy.sh
```

The script creates a Kind cluster named `core-banking` if needed, builds all seven images with the repository Dockerfile, loads them into Kind, and waits for the Helm release. Override `KIND_CLUSTER_NAME`, `KUBERNETES_NAMESPACE`, or `HELM_RELEASE_NAME` to use different names.

Forward the gateway and Keycloak ports in separate terminals:

```bash
kubectl -n core-banking port-forward svc/api-gateway 8080:8080
kubectl -n core-banking port-forward svc/keycloak 8081:8080
```

To inspect local email, also forward Mailpit:

```bash
kubectl -n core-banking port-forward svc/mailpit 8025:8025
```

Run the existing authenticated transfer demo from the repository root:

```bash
./scripts/demo-transfer.sh
```

The local Keycloak realm advertises `http://localhost:8081`; keep the Keycloak port-forward active while requesting tokens. Ledger remains cluster-internal. For local operator-only ledger inspection, use `kubectl -n core-banking port-forward svc/ledger-service 8085:8085`.

Local PostgreSQL initialization scripts run only on an empty data volume. To reset this disposable demo after changing initialization configuration, uninstall the release and delete its PVCs:

```bash
helm uninstall banking -n core-banking
kubectl delete pvc -n core-banking --all
```

## Cloud-oriented deployment

Use a dedicated namespace and supply versioned images from a registry reachable by the cluster. Set `imagePullSecrets` when the registry is private. Edit `values-cloud.yaml` to replace every example database hostname, Kafka endpoint, OIDC URL, ingress host, and `REPLACE_ME` image tag.

Create `core-banking-secrets` in the target namespace before installing the chart. It must contain these keys:

- `customer-db-password`, `wallet-db-password`, `transfer-db-password`, `ledger-db-password`, `loan-db-password`, `notification-db-password`
- `transfer-client-secret`, `wallet-client-secret`, `loan-client-secret`, `notification-client-secret`
- `kafka-sasl-jaas-config` with the broker provider's SASL JAAS value
- `smtp-username`, `smtp-password`

The cloud values demonstrate Kafka SASL_SSL/SCRAM-SHA-512 and SMTP STARTTLS/authentication. Override the Kafka Spring properties when the selected provider uses a different authentication mechanism. The database URLs use PostgreSQL `sslmode=require`. Use your cloud secret manager or approved external-secrets integration to maintain Kubernetes Secrets and rotate credentials.

The configured OIDC issuer must exactly match the token `iss` claim. The JWK and token endpoints must be reachable from the pods. Configure TLS at the ingress and configure the OIDC provider's public client redirect URIs for the actual application.

```bash
helm upgrade --install banking deploy/helm/core-banking \
  --namespace core-banking --create-namespace \
  --values deploy/helm/core-banking/values-cloud.yaml \
  --wait --timeout 10m
```

Optional CPU-based HPAs are controlled by `autoscaling.enabled`; the cluster must have a Metrics API. Database-backed ShedLock coordinates the loan repayment scheduler, transfer outbox publisher, and notification delivery poller when those deployments have multiple replicas. Select replica counts from load testing; the included requests, limits, and autoscaling values are starting points, not capacity claims.

Each service applies its own Flyway migrations during startup. Keep schema changes backward-compatible with the previous service image during rolling updates and rollbacks.

## Validation

The chart validation workflow runs Helm lint and renders both profiles on every push and pull request. The Kind script provides the local container deployment path. The in-cluster PostgreSQL, Kafka, and Keycloak resources are single-replica development dependencies and do not provide production availability guarantees.

For Jenkins CI/CD setup and required credentials, see [jenkins.md](jenkins.md).
