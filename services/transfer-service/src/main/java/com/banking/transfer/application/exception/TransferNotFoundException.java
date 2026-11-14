package com.banking.transfer.application.exception;

public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException() { super("Transfer not found"); }
}
