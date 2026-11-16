# ADR 0005: Loan lifecycle ownership and ledger authority

- Status: Accepted; local prototype policy defined, production policy remains open
- Date: 2026-11-14

## Context

Loan applications, automatic underwriting, contracts, fixed monthly installments, disbursements, and repayment attempts are closely related at this prototype scale. All money movement must preserve the Ledger's single-writer boundary.

## Decision

Loan Service owns application decisions, loan contracts, amortization schedules, installments, repayment attempts, and loan lifecycle status. It calls Ledger synchronously with stable idempotency IDs for disbursement and repayment; it never edits wallet balances. The local prototype uses deterministic approval rules, a fixed 12% APR, 3–36 month terms, configured principal/exposure limits, monthly amortization, and a final-installment rounding adjustment. Interest is recognized when a repayment is collected; insufficient-funds attempts remain overdue and retry daily.

## Consequences

Loan accounting remains balanced and atomic inside Ledger, while loan contractual state remains owned by Loan Service. The application, acceptance, disbursement, schedule, and repayment workflows are implemented and tested in the prototype. Real underwriting evidence, product disclosures, accrual accounting, delinquency policy, and applicable lending compliance remain outside this prototype and must be designed before real lending.
