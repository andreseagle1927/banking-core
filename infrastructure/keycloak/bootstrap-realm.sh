#!/bin/sh
set -eu

KCADM=/opt/keycloak/bin/kcadm.sh
REALM=${KEYCLOAK_REALM:-core-banking}
ADMIN_URL=${KEYCLOAK_ADMIN_URL:-http://keycloak:8080}

attempt=1
while [ "$attempt" -le 60 ]; do
  if "$KCADM" config credentials \
    --server "$ADMIN_URL" \
    --realm master \
    --user "$KEYCLOAK_ADMIN" \
    --password "$KEYCLOAK_ADMIN_PASSWORD" >/dev/null 2>&1; then
    break
  fi
  if [ "$attempt" -eq 60 ]; then
    echo "Keycloak admin API did not become ready" >&2
    exit 1
  fi
  sleep 2
  attempt=$((attempt + 1))
done

ensure_role() {
  role=$1
  if ! "$KCADM" get "roles/$role" -r "$REALM" >/dev/null 2>&1; then
    "$KCADM" create roles -r "$REALM" -s "name=$role"
  fi
}

ensure_client() {
  client=$1
  secret=$2
  client_id=$("$KCADM" get clients -r "$REALM" -q "clientId=$client" \
    | sed -n 's/^[[:space:]]*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)

  if [ -z "$client_id" ]; then
    "$KCADM" create clients -r "$REALM" \
      -s "clientId=$client" \
      -s enabled=true \
      -s protocol=openid-connect \
      -s publicClient=false \
      -s clientAuthenticatorType=client-secret \
      -s "secret=$secret" \
      -s serviceAccountsEnabled=true \
      -s standardFlowEnabled=false \
      -s directAccessGrantsEnabled=false
    client_id=$("$KCADM" get clients -r "$REALM" -q "clientId=$client" \
      | sed -n 's/^[[:space:]]*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)
  else
    "$KCADM" update "clients/$client_id" -r "$REALM" \
      -s enabled=true \
      -s protocol=openid-connect \
      -s publicClient=false \
      -s clientAuthenticatorType=client-secret \
      -s "secret=$secret" \
      -s serviceAccountsEnabled=true \
      -s standardFlowEnabled=false \
      -s directAccessGrantsEnabled=false
  fi

  if [ -z "$client_id" ]; then
    echo "Could not provision Keycloak client $client" >&2
    exit 1
  fi
}

ensure_service_role() {
  client=$1
  role=$2
  username="service-account-$client"
  assigned=$("$KCADM" get-roles -r "$REALM" --uusername "$username" --effective)
  if ! printf '%s' "$assigned" | grep -Fq "\"name\" : \"$role\""; then
    "$KCADM" add-roles -r "$REALM" --uusername "$username" --rolename "$role"
  fi
}

for role in ledger-poster wallet-provisioner wallet-reader loan-underwriter loan-poster notification-reader; do
  ensure_role "$role"
done

ensure_client wallet-service "${WALLET_CLIENT_SECRET:-wallet-local-secret}"
ensure_client loan-service "${LOAN_CLIENT_SECRET:-loan-local-secret}"
ensure_client notification-service "${NOTIFICATION_CLIENT_SECRET:-notification-local-secret}"
ensure_client transfer-service "${TRANSFER_CLIENT_SECRET:-transfer-local-secret}"

ensure_service_role wallet-service wallet-provisioner
ensure_service_role wallet-service wallet-reader
ensure_service_role loan-service loan-underwriter
ensure_service_role loan-service loan-poster
ensure_service_role notification-service notification-reader
ensure_service_role transfer-service ledger-poster

echo "Keycloak realm $REALM is ready for banking services"
