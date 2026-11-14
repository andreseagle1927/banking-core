package com.banking.notification.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findTop100ByRecipientWalletIdOrderByCreatedAtDesc(UUID walletId);
}
