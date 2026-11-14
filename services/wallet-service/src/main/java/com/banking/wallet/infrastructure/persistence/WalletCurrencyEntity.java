package com.banking.wallet.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "wallet_currencies", uniqueConstraints = @UniqueConstraint(columnNames = {"wallet_id", "currency"}))
public class WalletCurrencyEntity {
    @Id private UUID id;
    @Column(name = "wallet_id", nullable = false) private UUID walletId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected WalletCurrencyEntity() { }
}
