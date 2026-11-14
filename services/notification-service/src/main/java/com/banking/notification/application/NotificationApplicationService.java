package com.banking.notification.application;

import com.banking.notification.application.port.out.NotificationStore;
import com.banking.notification.domain.Notification;
import com.banking.notification.domain.TransferCompletedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationApplicationService {
    private static final String TRANSFER_COMPLETED = "TransferCompleted";
    private static final String NOTIFICATION_TYPE = "TRANSFER_COMPLETED";
    private final NotificationStore store;
    public NotificationApplicationService(NotificationStore store) { this.store = store; }

    @Transactional
    public void createTransferNotifications(TransferCompletedEvent event) {
        validate(event);
        if (!store.markEventProcessed(event.eventId(), event.eventType(), Instant.now())) return;
        Instant createdAt = Instant.now();
        store.saveNotification(new Notification(UUID.randomUUID(), event.aggregateId(), NOTIFICATION_TYPE,
                "Transfer of " + event.amount() + " " + event.currency() + " completed", createdAt), event.senderWalletId());
        store.saveNotification(new Notification(UUID.randomUUID(), event.aggregateId(), NOTIFICATION_TYPE,
                "You received " + event.amount() + " " + event.currency(), createdAt), event.recipientWalletId());
    }

    @Transactional(readOnly = true)
    public List<Notification> forWallet(UUID walletId) { return store.findRecentForWallet(walletId, 100); }

    private void validate(TransferCompletedEvent event) {
        if (event == null || event.eventId() == null || event.aggregateId() == null || event.senderWalletId() == null || event.recipientWalletId() == null)
            throw new IllegalArgumentException("Transfer event is missing required identifiers");
        if (!TRANSFER_COMPLETED.equals(event.eventType()) || event.schemaVersion() != 1)
            throw new IllegalArgumentException("Unsupported transfer event type or schema version");
        if (event.senderWalletId().equals(event.recipientWalletId())) throw new IllegalArgumentException("Transfer event parties must differ");
        BigDecimal amount;
        try { amount = new BigDecimal(event.amount()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid transfer event amount", ex); }
        if (amount.signum() <= 0 || amount.scale() > 2) throw new IllegalArgumentException("Invalid transfer event amount");
        if (event.currency() == null || !event.currency().matches("[A-Z]{3}")) throw new IllegalArgumentException("Invalid transfer event currency");
        try {
            if (Currency.getInstance(event.currency()).getDefaultFractionDigits() != 2) throw new IllegalArgumentException("Invalid transfer event currency");
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid transfer event currency", ex);
        }
    }
}
