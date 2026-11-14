package com.banking.ledger.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalTransactionJpaRepository extends JpaRepository<JournalTransactionEntity, UUID> {
    Optional<JournalTransactionEntity> findByOperationId(UUID operationId);
}
