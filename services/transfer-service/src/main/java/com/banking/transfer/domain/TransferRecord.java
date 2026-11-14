package com.banking.transfer.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRecord(UUID id, String requestHash, UUID senderWalletId, UUID recipientWalletId,
                             BigDecimal amount, String currency, TransferStatus status,
                             UUID ledgerTransactionId, String failureReason) {
    public static TransferRecord pending(UUID id, String requestHash, UUID sender, UUID recipient,
                                         Money money) {
        if (sender == null || recipient == null) throw new TransferRuleViolation("Sender and recipient are required");
        if (sender.equals(recipient)) throw new TransferRuleViolation("Sender and recipient must differ");
        return new TransferRecord(id, requestHash, sender, recipient, money.amount(), money.currency(),
                TransferStatus.PENDING, null, null);
    }
}
