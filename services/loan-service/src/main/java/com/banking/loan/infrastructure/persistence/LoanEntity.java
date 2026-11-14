package com.banking.loan.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "loans")
public class LoanEntity {
    @Id private UUID id;
    @Column(name = "application_id", nullable = false, unique = true) private UUID applicationId;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "principal_amount", nullable = false, precision = 18, scale = 2) private BigDecimal principalAmount;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable = false, length = 3) private String currency;
    @Column(name = "annual_rate", nullable = false, precision = 9, scale = 6) private BigDecimal annualRate;
    @Column(name = "term_months", nullable = false) private int termMonths;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected LoanEntity() { }
}
