package com.banking.transfer.infrastructure.persistence;

import com.banking.transfer.domain.TransferRecord;
import com.banking.transfer.domain.TransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transfers")
public class TransferEntity {
    @Id private UUID id;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128) private String idempotencyKey;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Column(name = "sender_wallet_id", nullable = false) private UUID senderWalletId;
    @Column(name = "recipient_wallet_id", nullable = false) private UUID recipientWalletId;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "ledger_transaction_id") private UUID ledgerTransactionId;
    @Column(name = "failure_reason") private String failureReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    protected TransferEntity() { }
    public TransferEntity(TransferRecord transfer, String key, Instant createdAt) {
        id = transfer.id(); idempotencyKey = key; requestHash = transfer.requestHash(); senderWalletId = transfer.senderWalletId();
        recipientWalletId = transfer.recipientWalletId(); amount = transfer.amount(); currency = transfer.currency();
        status = transfer.status().name(); ledgerTransactionId = transfer.ledgerTransactionId(); failureReason = transfer.failureReason();
        this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public String getStatus() { return status; }
    public UUID getSenderWalletId() { return senderWalletId; }
    public UUID getRecipientWalletId() { return recipientWalletId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getRequestHash() { return requestHash; }
    public UUID getLedgerTransactionId() { return ledgerTransactionId; }
    public String getFailureReason() { return failureReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public TransferRecord toDomain() {
        return new TransferRecord(id, requestHash, senderWalletId, recipientWalletId, amount, currency,
                TransferStatus.valueOf(status), ledgerTransactionId, failureReason);
    }
    public void reject(String reason, Instant at) { status = "REJECTED"; failureReason = reason; updatedAt = at; }
    public void complete(UUID ledgerId, Instant at) { status = "COMPLETED"; ledgerTransactionId = ledgerId; updatedAt = at; }
}
