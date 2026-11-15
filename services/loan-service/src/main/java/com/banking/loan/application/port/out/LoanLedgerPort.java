package com.banking.loan.application.port.out;
import java.math.BigDecimal;
import java.util.UUID;
public interface LoanLedgerPort {
    UUID disburse(UUID operationId,UUID loanId,UUID walletId,BigDecimal amount,String currency);
    UUID repay(UUID operationId,UUID loanId,UUID walletId,BigDecimal principal,BigDecimal interest,String currency);
    class InsufficientFunds extends RuntimeException { public InsufficientFunds(String message){super(message);} }
    class LedgerUnavailable extends RuntimeException { public LedgerUnavailable(String message,Throwable cause){super(message,cause);} }
}
