#!/usr/bin/env bash
set -euo pipefail

KEYCLOAK_URL=${KEYCLOAK_URL:-http://localhost:8081}
GATEWAY_URL=${GATEWAY_URL:-http://localhost:8080}
LEDGER_URL=${LEDGER_URL:-http://localhost:8085}
ALICE_ID=11111111-1111-4111-8111-111111111111
BOB_ID=22222222-2222-4222-8222-222222222222
TRANSFER_AMOUNT=${TRANSFER_AMOUNT:-100.00}
CURRENCY=${CURRENCY:-USD}

for dependency in curl python3; do
  command -v "$dependency" >/dev/null || { echo "Missing required command: $dependency" >&2; exit 1; }
done

json_field() {
  python3 -c 'import json,sys; value=json.load(sys.stdin).get(sys.argv[1]); print(value if value is not None else "")' "$1"
}

has_transfer_notification() {
  python3 -c 'import json,sys; items=json.load(sys.stdin); sys.exit(0 if any(item.get("transferId")==sys.argv[1] for item in items) else 1)' "$1"
}

token_for() {
  curl -fsS -X POST "$KEYCLOAK_URL/realms/core-banking/protocol/openid-connect/token" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode 'client_id=banking-local' \
    --data-urlencode 'grant_type=password' \
    --data-urlencode "username=$1" \
    --data-urlencode "password=$2" | json_field access_token
}

post_json() {
  local url=$1 token=$2 body=$3 response
  shift 3
  local -a headers=(-H "Authorization: Bearer $token" -H 'Content-Type: application/json')
  if [[ $# -gt 0 ]]; then headers+=("$@"); fi
  response=$(curl -sS -X POST "$url" "${headers[@]}" --data "$body" -w $'\n%{http_code}')
  HTTP_CODE=${response##*$'\n'}
  HTTP_BODY=${response%$'\n'*}
}

put_json() {
  local url=$1 token=$2 body=$3 response
  response=$(curl -sS -X PUT "$url" -H "Authorization: Bearer $token" -H 'Content-Type: application/json' --data "$body" -w $'\n%{http_code}')
  HTTP_CODE=${response##*$'\n'}
  HTTP_BODY=${response%$'\n'*}
}

ALICE_TOKEN=$(token_for alice "${ALICE_PASSWORD:-alice-local-password}")
BOB_TOKEN=$(token_for bob "${BOB_PASSWORD:-bob-local-password}")
OPERATOR_TOKEN=$(token_for operator "${OPERATOR_PASSWORD:-operator-local-password}")
FUNDING_OPERATION_ID=$(python3 -c 'import uuid; print(uuid.uuid4())')
IDEMPOTENCY_KEY="demo-$(python3 -c 'import uuid; print(uuid.uuid4())')"

post_json "$GATEWAY_URL/api/v1/customers/me" "$ALICE_TOKEN" '{"firstName":"Alice","lastName":"Demo","email":"alice@example.local"}'
[[ $HTTP_CODE == 201 ]] || { echo "Alice onboarding failed (HTTP $HTTP_CODE): $HTTP_BODY" >&2; exit 1; }
post_json "$GATEWAY_URL/api/v1/customers/me" "$BOB_TOKEN" '{"firstName":"Bob","lastName":"Demo","email":"bob@example.local"}'
[[ $HTTP_CODE == 201 ]] || { echo "Bob onboarding failed (HTTP $HTTP_CODE): $HTTP_BODY" >&2; exit 1; }
post_json "$GATEWAY_URL/api/v1/wallets/me" "$ALICE_TOKEN" ''
[[ $HTTP_CODE == 201 ]] || { echo "Alice wallet creation failed (HTTP $HTTP_CODE): $HTTP_BODY" >&2; exit 1; }
post_json "$GATEWAY_URL/api/v1/wallets/me" "$BOB_TOKEN" ''
[[ $HTTP_CODE == 201 ]] || { echo "Bob wallet creation failed (HTTP $HTTP_CODE): $HTTP_BODY" >&2; exit 1; }
put_json "$GATEWAY_URL/api/v1/wallets/me/currencies/$CURRENCY" "$ALICE_TOKEN" '{}'
[[ $HTTP_CODE == 200 ]] || { echo "Alice currency setup failed (HTTP $HTTP_CODE): $HTTP_BODY" >&2; exit 1; }
put_json "$GATEWAY_URL/api/v1/wallets/me/currencies/$CURRENCY" "$BOB_TOKEN" '{}'
[[ $HTTP_CODE == 200 ]] || { echo "Bob currency setup failed (HTTP $HTTP_CODE): $HTTP_BODY" >&2; exit 1; }
post_json "$LEDGER_URL/internal/ledger/funding" "$OPERATOR_TOKEN" \
  "{\"operationId\":\"$FUNDING_OPERATION_ID\",\"walletId\":\"$ALICE_ID\",\"amount\":\"500.00\",\"currency\":\"$CURRENCY\"}"
[[ $HTTP_CODE == 200 ]] || { echo "Simulated funding failed (HTTP $HTTP_CODE): $HTTP_BODY" >&2; exit 1; }

REQUEST_BODY="{\"recipientWalletId\":\"$BOB_ID\",\"amount\":\"$TRANSFER_AMOUNT\",\"currency\":\"$CURRENCY\"}"
for attempt in 1 2 3 4 5; do
  post_json "$GATEWAY_URL/api/v1/transfers" "$ALICE_TOKEN" "$REQUEST_BODY" \
    -H "Idempotency-Key: $IDEMPOTENCY_KEY"
  TRANSFER_STATUS=$(json_field status <<<"$HTTP_BODY")
  if [[ $TRANSFER_STATUS == COMPLETED ]]; then break; fi
  if [[ $TRANSFER_STATUS != PENDING ]]; then
    echo "Transfer failed (HTTP $HTTP_CODE): $HTTP_BODY" >&2
    exit 1
  fi
  sleep 1
done
if [[ ${TRANSFER_STATUS:-} != COMPLETED ]]; then
  echo "Transfer is still pending after safe retries: $HTTP_BODY" >&2
  exit 1
fi
TRANSFER_ID=$(json_field transferId <<<"$HTTP_BODY")

NOTIFICATIONS='[]'
for attempt in 1 2 3 4 5 6 7 8 9 10; do
  NOTIFICATIONS=$(curl -fsS "$GATEWAY_URL/api/v1/notifications/me" -H "Authorization: Bearer $BOB_TOKEN")
  if has_transfer_notification "$TRANSFER_ID" <<<"$NOTIFICATIONS"; then break; fi
  sleep 1
done
has_transfer_notification "$TRANSFER_ID" <<<"$NOTIFICATIONS" || {
  echo "Transfer completed but Bob's notification has not arrived yet" >&2
  exit 1
}

ALICE_BALANCE=$(curl -fsS "$LEDGER_URL/internal/ledger/accounts/$ALICE_ID/balances/$CURRENCY" -H "Authorization: Bearer $ALICE_TOKEN" | json_field balanceMinor)
BOB_BALANCE=$(curl -fsS "$LEDGER_URL/internal/ledger/accounts/$BOB_ID/balances/$CURRENCY" -H "Authorization: Bearer $BOB_TOKEN" | json_field balanceMinor)
python3 -c 'import json,sys; data=json.load(sys.stdin); print(json.dumps({key:data.get(key) for key in ("transferId","status","amount","currency","ledgerTransactionId")}, indent=2))' <<<"$HTTP_BODY"
printf 'Alice balance: %s minor units\nBob balance: %s minor units\n' "$ALICE_BALANCE" "$BOB_BALANCE"
