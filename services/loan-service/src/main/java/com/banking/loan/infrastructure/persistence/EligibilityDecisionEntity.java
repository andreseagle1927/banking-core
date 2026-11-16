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
    public EligibilityDecisionEntity(UUID id,UUID applicationId,String decision,String reason,String ruleVersion,Instant decidedAt){this.id=id;this.applicationId=applicationId;this.decision=decision;this.reason=reason;this.ruleVersion=ruleVersion;this.decidedAt=decidedAt;}
    public UUID getId(){return id;} public UUID getApplicationId(){return applicationId;} public String getDecision(){return decision;} public String getReason(){return reason;} public String getRuleVersion(){return ruleVersion;} public Instant getDecidedAt(){return decidedAt;}
}
