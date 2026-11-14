package com.banking.loan.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "installments", uniqueConstraints = @UniqueConstraint(columnNames = {"loan_id", "installment_number"}))
public class InstallmentEntity {
    @Id private UUID id;
    @Column(name = "loan_id", nullable = false) private UUID loanId;
    @Column(name = "installment_number", nullable = false) private int installmentNumber;
    @Column(name = "principal_due", nullable = false, precision = 18, scale = 2) private BigDecimal principalDue;
    @Column(name = "interest_due", nullable = false, precision = 18, scale = 2) private BigDecimal interestDue;
    @Column(name = "total_due", nullable = false, precision = 18, scale = 2) private BigDecimal totalDue;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(nullable = false, length = 16) private String status;
    protected InstallmentEntity() { }
}
