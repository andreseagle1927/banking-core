package com.banking.ledger.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerAccountJpaRepository extends JpaRepository<LedgerAccountEntity, UUID> {
    Optional<LedgerAccountEntity> findByOwnerIdAndCurrency(UUID ownerId, String currency);
    Optional<LedgerAccountEntity> findByOwnerIdAndCurrencyAndStatusAndAccountType(UUID ownerId, String currency, String status, String accountType);

    @Modifying
    @Query(value = "INSERT INTO ledger_accounts(id, owner_id, currency, account_type, status) VALUES (:id, :ownerId, :currency, :accountType, 'ACTIVE') ON CONFLICT (owner_id, currency) DO NOTHING", nativeQuery = true)
    int insertAccountIfAbsent(@Param("id") UUID id, @Param("ownerId") UUID ownerId,
                              @Param("currency") String currency, @Param("accountType") String accountType);

    @Query("select b.balanceMinor from AccountBalanceEntity b join LedgerAccountEntity a on a.id = b.ledgerAccountId where a.ownerId = :ownerId and a.currency = :currency and a.status = 'ACTIVE' and a.accountType = 'CUSTOMER_WALLET'")
    Optional<Long> findActiveWalletBalance(@Param("ownerId") UUID ownerId, @Param("currency") String currency);
}
