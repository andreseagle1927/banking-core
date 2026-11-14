package com.banking.ledger.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ledger_accounts")
public class LedgerAccountEntity {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable = false, length = 3) private String currency;
    @Column(name = "account_type", nullable = false, length = 32) private String accountType;
    @Column(nullable = false, length = 16) private String status;

    protected LedgerAccountEntity() { }

    public LedgerAccountEntity(UUID id, UUID ownerId, String currency, String accountType, String status) {
        this.id = id; this.ownerId = ownerId; this.currency = currency; this.accountType = accountType; this.status = status;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getCurrency() { return currency; }
    public String getAccountType() { return accountType; }
    public String getStatus() { return status; }
}
