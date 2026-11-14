package com.banking.loan.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EligibilityDecisionJpaRepository extends JpaRepository<EligibilityDecisionEntity, UUID> {
    Optional<EligibilityDecisionEntity> findByApplicationId(UUID applicationId);
}
