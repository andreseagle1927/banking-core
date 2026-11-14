package com.banking.transfer.application.port.out;

import com.banking.transfer.domain.TransferRecord;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository {
    TransferRecord savePendingIfAbsent(String idempotencyKey, TransferRecord pending);
    TransferRecord findByIdempotencyKey(String idempotencyKey);
    Optional<TransferRecord> findVisibleTo(UUID transferId, UUID walletId);
    TransferRecord findById(UUID transferId);
    void markRejected(UUID transferId, String reason);
}
