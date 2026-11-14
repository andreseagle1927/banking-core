package com.banking.transfer.infrastructure.persistence;

import com.banking.transfer.application.port.out.TransferRepository;
import com.banking.transfer.domain.TransferRecord;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTransferRepositoryAdapter implements TransferRepository {
    private final TransferJpaRepository transfers;
    public JpaTransferRepositoryAdapter(TransferJpaRepository transfers) { this.transfers = transfers; }

    @Override
    public TransferRecord savePendingIfAbsent(String key, TransferRecord pending) {
        Optional<TransferEntity> existing = transfers.findByIdempotencyKey(key);
        if (existing.isPresent()) return existing.get().toDomain();
        try {
            return transfers.saveAndFlush(new TransferEntity(pending, key, Instant.now())).toDomain();
        } catch (DataIntegrityViolationException concurrentRequest) {
            return transfers.findByIdempotencyKey(key).orElseThrow(() -> concurrentRequest).toDomain();
        }
    }

    @Override public TransferRecord findByIdempotencyKey(String key) {
        return transfers.findByIdempotencyKey(key).orElseThrow(() -> new IllegalStateException("Transfer not found")).toDomain();
    }

    @Override public Optional<TransferRecord> findVisibleTo(UUID id, UUID walletId) {
        return transfers.findVisibleTo(id, walletId).map(TransferEntity::toDomain);
    }

    @Override public TransferRecord findById(UUID id) {
        return transfers.findById(id).orElseThrow(() -> new IllegalStateException("Transfer not found")).toDomain();
    }

    @Override public void markRejected(UUID id, String reason) {
        transfers.findById(id).ifPresent(transfer -> {
            transfer.reject(reason, Instant.now());
            transfers.save(transfer);
        });
    }
}
