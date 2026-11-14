package com.banking.wallet.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "wallets")
public class WalletEntity {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false, unique = true) private UUID customerId;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected WalletEntity() { }
}
