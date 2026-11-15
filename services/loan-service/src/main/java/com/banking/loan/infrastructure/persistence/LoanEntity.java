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
    @Column(name = "ledger_transaction_id") private UUID ledgerTransactionId;
    protected LoanEntity() { }
    public LoanEntity(UUID id,UUID applicationId,UUID customerId,BigDecimal principalAmount,String currency,BigDecimal annualRate,int termMonths,String status,Instant createdAt,UUID ledgerTransactionId){this.id=id;this.applicationId=applicationId;this.customerId=customerId;this.principalAmount=principalAmount;this.currency=currency;this.annualRate=annualRate;this.termMonths=termMonths;this.status=status;this.createdAt=createdAt;this.ledgerTransactionId=ledgerTransactionId;}
    public UUID getId(){return id;} public UUID getApplicationId(){return applicationId;} public UUID getCustomerId(){return customerId;} public BigDecimal getPrincipalAmount(){return principalAmount;} public String getCurrency(){return currency;} public BigDecimal getAnnualRate(){return annualRate;} public int getTermMonths(){return termMonths;} public String getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public UUID getLedgerTransactionId(){return ledgerTransactionId;} public void setStatus(String status){this.status=status;}
}
