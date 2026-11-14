package com.banking.notification.domain;

import java.time.Instant;
import java.util.UUID;

public record TransferCompletedEvent(UUID eventId, String eventType, int schemaVersion, UUID aggregateId,
                                     Instant occurredAt, UUID correlationId, UUID senderWalletId,
                                     UUID recipientWalletId, String amount, String currency,
                                     UUID ledgerTransactionId) { }
