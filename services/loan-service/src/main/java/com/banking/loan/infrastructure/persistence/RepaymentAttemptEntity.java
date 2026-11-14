package com.banking.loan.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "repayment_attempts")
public class RepaymentAttemptEntity {
    @Id private UUID id;
    @Column(name = "installment_id", nullable = false) private UUID installmentId;
    @Column(name = "operation_id", nullable = false, unique = true) private UUID operationId;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "attempted_at", nullable = false) private Instant attemptedAt;
    @Column(name = "failure_reason") private String failureReason;
    protected RepaymentAttemptEntity() { }
}
