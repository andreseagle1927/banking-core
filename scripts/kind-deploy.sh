#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLUSTER_NAME="${KIND_CLUSTER_NAME:-core-banking}"
NAMESPACE="${KUBERNETES_NAMESPACE:-core-banking}"
RELEASE_NAME="${HELM_RELEASE_NAME:-banking}"
CHART_DIR="$ROOT_DIR/deploy/helm/core-banking"
VALUES_FILE="$CHART_DIR/values-local.yaml"

for command in docker kind kubectl helm; do
  command -v "$command" >/dev/null || {
    echo "Required command not found: $command" >&2
    exit 1
  }
done

if ! kind get clusters | grep -Fxq "$CLUSTER_NAME"; then
  kind create cluster --name "$CLUSTER_NAME"
fi
kubectl config use-context "kind-$CLUSTER_NAME"

while IFS=' ' read -r image module port; do
  [[ -z "$image" ]] && continue
  echo "Building $image"
  docker build --pull \
    --build-arg "MODULE=$module" \
    --build-arg "APP_PORT=$port" \
    --tag "core-banking/$image:local" \
    "$ROOT_DIR"
  kind load docker-image "core-banking/$image:local" --name "$CLUSTER_NAME"
done <<'IMAGES'
api-gateway gateway/api-gateway 8080
customer-service services/customer-service 8082
wallet-service services/wallet-service 8083
transfer-service services/transfer-service 8084
ledger-service services/ledger-service 8085
loan-service services/loan-service 8086
notification-service services/notification-service 8087
IMAGES

helm upgrade --install "$RELEASE_NAME" "$CHART_DIR" \
  --namespace "$NAMESPACE" \
  --create-namespace \
  --values "$VALUES_FILE" \
  --wait \
  --timeout 15m

kubectl --namespace "$NAMESPACE" get deployments,statefulsets,pods,services
