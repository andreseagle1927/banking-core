package com.banking.transfer.application.exception;

public class LedgerUnavailableException extends RuntimeException {
    public LedgerUnavailableException(Throwable cause) { super("Ledger response unavailable", cause); }
}
