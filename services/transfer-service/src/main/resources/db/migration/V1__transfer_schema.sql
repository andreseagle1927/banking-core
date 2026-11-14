CREATE TABLE transfers (
  id UUID PRIMARY KEY,
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  request_hash CHAR(64) NOT NULL,
  sender_wallet_id UUID NOT NULL,
  recipient_wallet_id UUID NOT NULL,
  amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
  currency CHAR(3) NOT NULL,
  status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','COMPLETED','REJECTED')),
  ledger_transaction_id UUID,
  failure_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ
);
CREATE INDEX transfers_sender_idx ON transfers(sender_wallet_id, created_at DESC);
CREATE INDEX transfers_recipient_idx ON transfers(recipient_wallet_id, created_at DESC);
CREATE TABLE outbox_events (
  event_id UUID PRIMARY KEY,
  aggregate_id UUID NOT NULL,
  event_type VARCHAR(128) NOT NULL,
  schema_version INTEGER NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL,
  payload JSONB NOT NULL,
  published_at TIMESTAMPTZ
);
CREATE INDEX outbox_unpublished_idx ON outbox_events(occurred_at) WHERE published_at IS NULL;
