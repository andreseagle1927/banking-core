CREATE TABLE notification_deliveries (
  id UUID PRIMARY KEY,
  notification_id UUID NOT NULL UNIQUE REFERENCES notifications(id),
  recipient_email VARCHAR(254) NOT NULL,
  subject VARCHAR(200) NOT NULL,
  message VARCHAR(1000) NOT NULL,
  status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','SENT','FAILED')),
  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  created_at TIMESTAMPTZ NOT NULL,
  next_attempt_at TIMESTAMPTZ NOT NULL,
  attempted_at TIMESTAMPTZ,
  last_error VARCHAR(500)
);
CREATE INDEX notification_deliveries_pending_idx ON notification_deliveries(status,next_attempt_at,created_at);
