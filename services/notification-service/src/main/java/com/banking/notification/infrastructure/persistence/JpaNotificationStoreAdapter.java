package com.banking.notification.infrastructure.persistence;

import com.banking.notification.application.port.out.NotificationStore;
import com.banking.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaNotificationStoreAdapter implements NotificationStore {
    private final ProcessedEventJpaRepository processedEvents;
    private final NotificationJpaRepository notifications;

    public JpaNotificationStoreAdapter(ProcessedEventJpaRepository processedEvents, NotificationJpaRepository notifications) {
        this.processedEvents = processedEvents; this.notifications = notifications;
    }

    @Override public boolean markEventProcessed(UUID eventId, String eventType, Instant processedAt) {
        return processedEvents.insertIfAbsent(eventId, eventType, processedAt) == 1;
    }

    @Override public void saveNotification(Notification notification, UUID recipientWalletId) {
        notifications.save(new NotificationEntity(notification, recipientWalletId));
    }

    @Override public List<Notification> findRecentForWallet(UUID walletId, int limit) {
        return notifications.findTop100ByRecipientWalletIdOrderByCreatedAtDesc(walletId).stream()
                .limit(Math.min(Math.max(limit, 0), 100)).map(NotificationEntity::toDomain).toList();
    }
}
