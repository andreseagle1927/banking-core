package com.banking.transfer.application.exception;

public class LedgerCallException extends RuntimeException {
    private final int statusCode;
    public LedgerCallException(int statusCode, String message) { super(message); this.statusCode = statusCode; }
    public int statusCode() { return statusCode; }
}
