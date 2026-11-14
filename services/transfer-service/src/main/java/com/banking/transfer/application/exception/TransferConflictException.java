package com.banking.transfer.application.exception;

public class TransferConflictException extends RuntimeException {
    public TransferConflictException(String message) { super(message); }
}
