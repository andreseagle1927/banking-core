CREATE TABLE wallets (
  id UUID PRIMARY KEY,
  customer_id UUID NOT NULL UNIQUE,
  status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','BLOCKED','CLOSED')),
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE wallet_currencies (
  id UUID PRIMARY KEY,
  wallet_id UUID NOT NULL REFERENCES wallets(id),
  currency CHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
  status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','BLOCKED','CLOSED')),
  created_at TIMESTAMPTZ NOT NULL,
  UNIQUE (wallet_id, currency)
);
