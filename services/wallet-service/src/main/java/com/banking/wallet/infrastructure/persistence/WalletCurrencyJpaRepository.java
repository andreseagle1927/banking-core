package com.banking.wallet.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletCurrencyJpaRepository extends JpaRepository<WalletCurrencyEntity, UUID> {
    List<WalletCurrencyEntity> findByWalletId(UUID walletId);
    java.util.Optional<WalletCurrencyEntity> findByWalletIdAndCurrency(UUID walletId, String currency);
    List<WalletCurrencyEntity> findByWalletIdOrderByCurrency(UUID walletId);
    List<WalletCurrencyEntity> findByWalletIdAndStatus(UUID walletId, String status);
}
