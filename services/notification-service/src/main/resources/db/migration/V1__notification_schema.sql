CREATE TABLE processed_events (
  event_id UUID PRIMARY KEY,
  event_type VARCHAR(128) NOT NULL,
  processed_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE notifications (
  id UUID PRIMARY KEY,
  recipient_wallet_id UUID NOT NULL,
  transfer_id UUID NOT NULL,
  notification_type VARCHAR(64) NOT NULL,
  message VARCHAR(500) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX notifications_wallet_created_idx ON notifications(recipient_wallet_id, created_at DESC);
