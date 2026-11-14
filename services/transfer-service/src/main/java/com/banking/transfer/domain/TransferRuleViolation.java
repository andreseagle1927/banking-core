package com.banking.transfer.domain;

public class TransferRuleViolation extends RuntimeException {
    public TransferRuleViolation(String message) { super(message); }
}
