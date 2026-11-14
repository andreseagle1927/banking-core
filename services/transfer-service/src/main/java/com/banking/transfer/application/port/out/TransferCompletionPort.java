package com.banking.transfer.application.port.out;

import java.util.UUID;

public interface TransferCompletionPort {
    void markCompletedAndEnqueueEvent(UUID transferId, UUID ledgerTransactionId);
}
