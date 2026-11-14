CREATE TABLE ledger_accounts (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL,
  currency CHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
  account_type VARCHAR(32) NOT NULL CHECK (account_type IN ('CUSTOMER_WALLET','PLATFORM_FUNDING')),
  status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','BLOCKED','CLOSED')),
  UNIQUE (owner_id, currency)
);
CREATE TABLE journal_transactions (
  id UUID PRIMARY KEY,
  operation_id UUID NOT NULL UNIQUE,
  transaction_type VARCHAR(32) NOT NULL,
  request_hash CHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE account_balances (
  ledger_account_id UUID PRIMARY KEY REFERENCES ledger_accounts(id),
  balance_minor BIGINT NOT NULL,
  version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE journal_entries (
  id UUID PRIMARY KEY,
  journal_transaction_id UUID NOT NULL REFERENCES journal_transactions(id),
  ledger_account_id UUID NOT NULL REFERENCES ledger_accounts(id),
  debit_minor BIGINT NOT NULL DEFAULT 0 CHECK (debit_minor >= 0),
  credit_minor BIGINT NOT NULL DEFAULT 0 CHECK (credit_minor >= 0),
  CHECK ((debit_minor = 0) <> (credit_minor = 0))
);
CREATE INDEX journal_entries_account_idx ON journal_entries(ledger_account_id);
CREATE FUNCTION forbid_journal_entry_mutation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'journal entries are append-only'; END; $$;
CREATE TRIGGER journal_entries_append_only BEFORE UPDATE OR DELETE ON journal_entries FOR EACH ROW EXECUTE FUNCTION forbid_journal_entry_mutation();
