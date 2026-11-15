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
    public InstallmentEntity(UUID id,UUID loanId,int installmentNumber,BigDecimal principalDue,BigDecimal interestDue,BigDecimal totalDue,LocalDate dueDate,String status){this.id=id;this.loanId=loanId;this.installmentNumber=installmentNumber;this.principalDue=principalDue;this.interestDue=interestDue;this.totalDue=totalDue;this.dueDate=dueDate;this.status=status;}
    public UUID getId(){return id;} public UUID getLoanId(){return loanId;} public int getInstallmentNumber(){return installmentNumber;} public BigDecimal getPrincipalDue(){return principalDue;} public BigDecimal getInterestDue(){return interestDue;} public BigDecimal getTotalDue(){return totalDue;} public LocalDate getDueDate(){return dueDate;} public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
}
