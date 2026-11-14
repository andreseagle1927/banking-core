package com.banking.notification.application.port.out;

import com.banking.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationStore {
    boolean markEventProcessed(UUID eventId, String eventType, Instant processedAt);
    void saveNotification(Notification notification, UUID recipientWalletId);
    List<Notification> findRecentForWallet(UUID walletId, int limit);
}
