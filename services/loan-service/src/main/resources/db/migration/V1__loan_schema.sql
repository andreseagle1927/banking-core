CREATE TABLE loan_applications (
  id UUID PRIMARY KEY,
  customer_id UUID NOT NULL,
  requested_amount NUMERIC(18,2) NOT NULL CHECK (requested_amount > 0),
  currency CHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
  term_months INTEGER NOT NULL CHECK (term_months > 0),
  status VARCHAR(24) NOT NULL CHECK (status IN ('SUBMITTED','APPROVED','REJECTED','ACCEPTED','CANCELLED')),
  submitted_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE eligibility_decisions (
  id UUID PRIMARY KEY,
  application_id UUID NOT NULL UNIQUE REFERENCES loan_applications(id),
  decision VARCHAR(16) NOT NULL CHECK (decision IN ('APPROVED','REJECTED')),
  reason TEXT NOT NULL,
  rule_version VARCHAR(64) NOT NULL,
  decided_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE loans (
  id UUID PRIMARY KEY,
  application_id UUID NOT NULL UNIQUE REFERENCES loan_applications(id),
  customer_id UUID NOT NULL,
  principal_amount NUMERIC(18,2) NOT NULL CHECK (principal_amount > 0),
  currency CHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
  annual_rate NUMERIC(9,6) NOT NULL CHECK (annual_rate >= 0),
  term_months INTEGER NOT NULL CHECK (term_months > 0),
  status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','PAID','DEFAULTED','CANCELLED')),
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE installments (
  id UUID PRIMARY KEY,
  loan_id UUID NOT NULL REFERENCES loans(id),
  installment_number INTEGER NOT NULL CHECK (installment_number > 0),
  principal_due NUMERIC(18,2) NOT NULL CHECK (principal_due >= 0),
  interest_due NUMERIC(18,2) NOT NULL CHECK (interest_due >= 0),
  total_due NUMERIC(18,2) NOT NULL CHECK (total_due > 0),
  due_date DATE NOT NULL,
  status VARCHAR(16) NOT NULL CHECK (status IN ('SCHEDULED','PAID','OVERDUE','CANCELLED')),
  UNIQUE (loan_id, installment_number)
);

CREATE TABLE repayment_attempts (
  id UUID PRIMARY KEY,
  installment_id UUID NOT NULL REFERENCES installments(id),
  operation_id UUID NOT NULL UNIQUE,
  amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
  status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','SUCCEEDED','FAILED')),
  attempted_at TIMESTAMPTZ NOT NULL,
  failure_reason TEXT
);
