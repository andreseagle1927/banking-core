package com.banking.loan.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "eligibility_decisions")
public class EligibilityDecisionEntity {
    @Id private UUID id;
    @Column(name = "application_id", nullable = false, unique = true) private UUID applicationId;
    @Column(nullable = false, length = 16) private String decision;
    @Column(nullable = false) private String reason;
    @Column(name = "rule_version", nullable = false, length = 64) private String ruleVersion;
    @Column(name = "decided_at", nullable = false) private Instant decidedAt;
    protected EligibilityDecisionEntity() { }
}
