package com.banking.transfer.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.banking.transfer.application.port.out.TransferCompletionPort;
import java.time.Instant;
import java.util.UUID;
import com.banking.transfer.infrastructure.persistence.OutboxEventEntity;
import com.banking.transfer.infrastructure.persistence.OutboxJpaRepository;
import com.banking.transfer.infrastructure.persistence.TransferEntity;
import com.banking.transfer.infrastructure.persistence.TransferJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferOutbox implements TransferCompletionPort {
    private final TransferJpaRepository transfers;
    private final OutboxJpaRepository outbox;
    private final ObjectMapper objectMapper;
    public TransferOutbox(TransferJpaRepository transfers, OutboxJpaRepository outbox, ObjectMapper objectMapper) {
        this.transfers = transfers; this.outbox = outbox; this.objectMapper = objectMapper;
    }

    @Transactional
    @Override
    public void markCompletedAndEnqueueEvent(UUID transferId, UUID ledgerTransactionId) {
        TransferEntity transfer = transfers.findPendingForUpdate(transferId).orElse(null);
        if (transfer == null) return;
        Instant now = Instant.now();
        UUID eventId = UUID.randomUUID();
        TransferCompletedEvent event = new TransferCompletedEvent(eventId, "TransferCompleted", 1, transferId, now, transferId,
                transfer.getSenderWalletId(), transfer.getRecipientWalletId(), transfer.getAmount().toPlainString(), transfer.getCurrency(), ledgerTransactionId);
        var payload = objectMapper.valueToTree(event);
        transfer.complete(ledgerTransactionId, now);
        outbox.save(new OutboxEventEntity(eventId, transferId, event.eventType(), event.schemaVersion(), now, payload));
    }
}
