# Private server demo

The banking demo is deployed on the single-node Kubernetes host in namespace `core-banking-demo` as Helm release `banking-demo`. The seven application images were built on the host from the customer, wallet, and loan workflow branch. The chart enables its local PostgreSQL, Kafka, Keycloak, and Mailpit dependencies.

## Open the demo

The gateway and identity-provider port forwards bind to loopback on the server. Start an SSH tunnel from your workstation:

```sh
ssh -N \
  -L 28181:127.0.0.1:28082 \
  -L 28081:127.0.0.1:28081 \
  andres@135.181.182.60
```

- Swagger UI: <http://localhost:28181/swagger-ui.html>
- Gateway health: <http://localhost:28181/actuator/health>
- Keycloak: <http://localhost:28081>, realm `core-banking`

The test realm contains the `alice`, `bob`, and `operator` demo users. Their development-only passwords are in `deploy/helm/core-banking/files/realm-core-banking.json`; the local Keycloak admin password is defined in `values-local.yaml`. These credentials are public examples and must not be reused.

The server runs `banking-demo-gateway-forward` and `banking-demo-keycloak-forward` systemd services. Both listen only on `127.0.0.1`; there is no public Ingress, NodePort, or Nginx route for the demo.

## Verify on the host

```sh
sudo k8s kubectl -n core-banking-demo get pods
sudo k8s kubectl -n core-banking-demo get services
sudo systemctl status banking-demo-gateway-forward banking-demo-keycloak-forward
```

The gateway Service uses ClusterIP `10.152.183.250`. Persistent data is stored on the host's default rawfile CSI volumes; the demo runs on one node.

## Production automation boundary

This is a private development demo, not the production deployment. It uses the chart's development credentials and in-cluster dependencies. Production deployment through Jenkins still requires the registry credential with GHCR package-write access, production kubeconfig and Helm values, managed database/Kafka/OIDC/SMTP credentials, and the `core-banking-secrets` Kubernetes Secret. See [jenkins.md](jenkins.md) and [kubernetes.md](kubernetes.md) before configuring those values. The Jenkins UI remains loopback-only at `http://localhost:28080` through its SSH tunnel.
