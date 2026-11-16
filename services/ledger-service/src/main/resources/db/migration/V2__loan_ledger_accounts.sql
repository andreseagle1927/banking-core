ALTER TABLE ledger_accounts DROP CONSTRAINT ledger_accounts_account_type_check;
ALTER TABLE ledger_accounts ADD CONSTRAINT ledger_accounts_account_type_check
  CHECK (account_type IN ('CUSTOMER_WALLET','PLATFORM_FUNDING','LOAN_RECEIVABLE','INTEREST_INCOME'));
