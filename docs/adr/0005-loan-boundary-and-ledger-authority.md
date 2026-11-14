# ADR 0005: Loan lifecycle ownership and ledger authority

- Status: Accepted for service boundaries; loan policy details remain open
- Date: 2026-11-14

## Context

Loan applications, automatic underwriting, contracts, fixed monthly installments, disbursements, and repayment attempts are closely related at this prototype scale. All money movement must preserve the Ledger's single-writer boundary.

## Decision

Loan Service owns application decisions, loan contracts, amortization schedules, installments, repayment attempts, and loan lifecycle status. It calls Ledger synchronously with stable idempotency IDs for disbursement and repayment; it never edits wallet balances. V1 target repayment schedules are fixed monthly installments, and approval uses deterministic rules over defined/simulated inputs. Lending rates, products, evidence requirements, final installment rounding, overdue/retry behavior, and interest accrual/recognition rules must be explicitly defined before loan posting behavior is implemented.

## Consequences

Loan accounting remains balanced and atomic inside Ledger, while loan contractual state remains owned by Loan Service. The service is currently only a health-checked API skeleton; no underwriting, contract, schedule, disbursement, or repayment code is implemented.
