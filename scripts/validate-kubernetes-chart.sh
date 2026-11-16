#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHART_DIR="$ROOT_DIR/deploy/helm/core-banking"
LOCAL_RENDER="$(mktemp)"
CLOUD_RENDER="$(mktemp)"
trap 'rm -f "$LOCAL_RENDER" "$CLOUD_RENDER"' EXIT

cmp "$ROOT_DIR/infrastructure/postgres/init-databases.sql" "$CHART_DIR/files/init-databases.sql"
cmp "$ROOT_DIR/infrastructure/keycloak/realm-core-banking.json" "$CHART_DIR/files/realm-core-banking.json"
cmp "$ROOT_DIR/infrastructure/keycloak/bootstrap-realm.sh" "$CHART_DIR/files/bootstrap-realm.sh"

helm lint "$CHART_DIR" --values "$CHART_DIR/values-local.yaml"
helm lint "$CHART_DIR" --values "$CHART_DIR/values-cloud.yaml"
helm template banking "$CHART_DIR" --values "$CHART_DIR/values-local.yaml" > "$LOCAL_RENDER"
helm template banking "$CHART_DIR" --values "$CHART_DIR/values-cloud.yaml" > "$CLOUD_RENDER"

for service in api-gateway customer-service wallet-service transfer-service ledger-service loan-service notification-service; do
  grep -Fq "name: $service" "$LOCAL_RENDER" || {
    echo "Local render is missing $service" >&2
    exit 1
  }
done

grep -Fq 'kind: StatefulSet' "$LOCAL_RENDER"
grep -Fq 'kind: Ingress' "$CLOUD_RENDER"
if grep -Fq 'name: postgres' "$CLOUD_RENDER" || grep -Fq 'name: keycloak' "$CLOUD_RENDER" || grep -Fq 'name: mailpit' "$CLOUD_RENDER"; then
  echo "Cloud render unexpectedly contains a local dependency" >&2
  exit 1
fi

echo "Both Kubernetes profiles linted and rendered successfully."
