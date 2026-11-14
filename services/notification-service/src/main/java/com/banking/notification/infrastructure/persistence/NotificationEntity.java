package com.banking.notification.infrastructure.persistence;

import com.banking.notification.domain.Notification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id private UUID id;
    @Column(name = "recipient_wallet_id", nullable = false) private UUID recipientWalletId;
    @Column(name = "transfer_id", nullable = false) private UUID transferId;
    @Column(name = "notification_type", nullable = false, length = 64) private String notificationType;
    @Column(nullable = false, length = 500) private String message;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected NotificationEntity() { }
    public NotificationEntity(Notification notification, UUID recipientWalletId) {
        this.id = notification.id(); this.recipientWalletId = recipientWalletId; this.transferId = notification.transferId();
        this.notificationType = notification.type(); this.message = notification.message(); this.createdAt = notification.createdAt();
    }
    public Notification toDomain() { return new Notification(id, transferId, notificationType, message, createdAt); }
}
