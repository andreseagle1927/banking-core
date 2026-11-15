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
    @Column(name = "idempotency_key", nullable = false, length = 128) private String idempotencyKey;
    protected LoanApplicationEntity() { }
    public LoanApplicationEntity(UUID id,UUID customerId,BigDecimal requestedAmount,String currency,int termMonths,String status,Instant submittedAt,String idempotencyKey) { this.id=id;this.customerId=customerId;this.requestedAmount=requestedAmount;this.currency=currency;this.termMonths=termMonths;this.status=status;this.submittedAt=submittedAt;this.idempotencyKey=idempotencyKey; }
    public UUID getId(){return id;} public UUID getCustomerId(){return customerId;} public BigDecimal getRequestedAmount(){return requestedAmount;}
    public String getIdempotencyKey(){return idempotencyKey;}
    public String getCurrency(){return currency;} public int getTermMonths(){return termMonths;} public String getStatus(){return status;} public Instant getSubmittedAt(){return submittedAt;}
    public void setStatus(String status){this.status=status;}
}
