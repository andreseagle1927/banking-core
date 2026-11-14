package com.banking.ledger.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountBalanceJpaRepository extends JpaRepository<AccountBalanceEntity, UUID> {
    @Modifying
    @Query(value = "INSERT INTO account_balances(ledger_account_id, balance_minor, version) VALUES (:accountId, 0, 0) ON CONFLICT (ledger_account_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("accountId") UUID accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from AccountBalanceEntity b where b.ledgerAccountId = :accountId")
    Optional<AccountBalanceEntity> findLockedByAccountId(@Param("accountId") UUID accountId);
}
