package com.banking.ledger.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "journal_transactions")
public class JournalTransactionEntity {
    @Id private UUID id;
    @Column(name = "operation_id", nullable = false, unique = true) private UUID operationId;
    @Column(name = "transaction_type", nullable = false, length = 32) private String transactionType;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected JournalTransactionEntity() { }
    public JournalTransactionEntity(UUID id, UUID operationId, String type, String requestHash, Instant createdAt) {
        this.id = id; this.operationId = operationId; this.transactionType = type; this.requestHash = requestHash; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getOperationId() { return operationId; }
    public String getTransactionType() { return transactionType; }
    public String getRequestHash() { return requestHash; }
}
