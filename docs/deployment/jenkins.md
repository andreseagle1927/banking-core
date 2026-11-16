# Jenkins CI/CD

The root `Jenkinsfile` is intended for a Jenkins Multibranch Pipeline running on the deployment host. Jenkins builds trusted repository branches and Git tags. GitHub Actions remains the pull-request gate; do not configure Jenkins to discover pull requests from forks because its agent uses the host Docker daemon. Jenkins publishes SHA-tagged images for `main` and Git tags, then deploys `main` to the `core-banking` Kubernetes namespace.

## Jenkins host

Jenkins LTS runs on `andres@135.181.182.60` with Java 21. The web UI is bound to `127.0.0.1:18080`; access it through an SSH tunnel instead of exposing the administrator UI publicly:

```sh
ssh -N -L 18080:127.0.0.1:18080 andres@135.181.182.60
```

Then open `http://localhost:18080`. For the first Jenkins setup, read the one-time unlock value directly on the server with `sudo cat /var/lib/jenkins/secrets/initialAdminPassword`; create a personal administrator account in the setup wizard. Configure a two-minute Multibranch scan so `main` deployments do not depend on a public Jenkins webhook.

The build node provides Java 21, Maven 3.9+, Docker CLI with access to the host Docker daemon, and Helm 4. The deployment stage uses Helm with its Jenkins kubeconfig credential; it does not require the privileged `k8s kubectl` wrapper. The Pipeline, Multibranch Pipeline, GitHub Branch Source, Credentials Binding, JUnit, and Workspace Cleanup plugins are installed. Run only trusted branches and tags on this host.

## Jenkins credentials

Configure these credentials before enabling the `main` deployment stage:

| Credential ID | Jenkins type | Contents |
| --- | --- | --- |
| `core-banking-registry` | Username with password | GHCR username and a token allowed to publish the seven image packages |
| `core-banking-prod-kubeconfig` | Secret file | Kubeconfig for the deployment cluster and `core-banking` namespace |
| `core-banking-prod-values` | Secret file | Production Helm overrides for the managed PostgreSQL, Kafka, OIDC, and SMTP endpoints; GHCR pull secret reference; and gateway ClusterIP settings |

Create the Kubernetes Secret named `core-banking-secrets` and the GHCR image-pull Secret in the `core-banking` namespace through the approved secret-management process. The application Secret must contain the keys listed in [kubernetes.md](kubernetes.md#cloud-oriented-deployment). Do not store service passwords in Git, the Jenkinsfile, Helm values, or build logs.

The production values must disable the chart Ingress because the host Nginx terminates TLS for `banking.adrenocromo.com`. Set the gateway Service to ClusterIP `10.152.183.250` after confirming that address is unused in the upgraded cluster. Nginx proxies to that Service IP; do not expose a Kubernetes NodePort.

## Deployment behavior

- GitHub Actions runs Maven, image-build, and chart checks for pull requests, including fork PRs. Jenkins does not execute fork PR code on the production host.
- Trusted branches run Maven verification, Helm validation, and all seven image builds without publishing or deploying.
- `main` publishes commit-SHA-tagged images and automatically runs `helm upgrade --install --atomic --wait`; Helm rolls back a failed rollout.
- Git tags publish commit-SHA-tagged images and do not deploy.
- The production build uses the `values-cloud.yaml` profile plus the secure production values file. The chart's local PostgreSQL, Kafka, Keycloak, and Mailpit dependencies remain disabled.

The app is deployed to a single Kubernetes node, so a host outage takes the app offline. This delivery configures automation and routing; it does not make the banking prototype highly available or certify it for regulated financial use.
