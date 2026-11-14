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

@Entity @Table(name = "loan_applications")
public class LoanApplicationEntity {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "requested_amount", nullable = false, precision = 18, scale = 2) private BigDecimal requestedAmount;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable = false, length = 3) private String currency;
    @Column(name = "term_months", nullable = false) private int termMonths;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "submitted_at", nullable = false) private Instant submittedAt;
    protected LoanApplicationEntity() { }
}
