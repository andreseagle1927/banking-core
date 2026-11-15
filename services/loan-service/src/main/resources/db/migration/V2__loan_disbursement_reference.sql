ALTER TABLE loans ADD COLUMN ledger_transaction_id UUID;
ALTER TABLE loan_applications ADD COLUMN idempotency_key VARCHAR(128);
UPDATE loan_applications SET idempotency_key = id::text WHERE idempotency_key IS NULL;
ALTER TABLE loan_applications ALTER COLUMN idempotency_key SET NOT NULL;
ALTER TABLE loan_applications ADD CONSTRAINT loan_applications_customer_idempotency_key UNIQUE (customer_id,idempotency_key);
