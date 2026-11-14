package com.banking.transfer.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferJpaRepository extends JpaRepository<TransferEntity, UUID> {
    Optional<TransferEntity> findByIdempotencyKey(String idempotencyKey);
    @Query("select t from TransferEntity t where t.id = :id and (t.senderWalletId = :walletId or t.recipientWalletId = :walletId)")
    Optional<TransferEntity> findVisibleTo(@Param("id") UUID id, @Param("walletId") UUID walletId);
    Optional<TransferEntity> findByIdAndStatus(UUID id, String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransferEntity t where t.id = :id and t.status = 'PENDING'")
    Optional<TransferEntity> findPendingForUpdate(@Param("id") UUID id);
}
