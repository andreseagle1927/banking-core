package com.banking.loan.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanJpaRepository extends JpaRepository<LoanEntity, UUID> {
    List<LoanEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    java.util.Optional<LoanEntity> findByCustomerIdAndId(UUID customerId,UUID id);
    java.util.Optional<LoanEntity> findByApplicationId(UUID applicationId);
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(l.principalAmount),0) from LoanEntity l where l.customerId=:customerId and l.status='ACTIVE'")
    java.math.BigDecimal activePrincipalExposure(@org.springframework.data.repository.query.Param("customerId") UUID customerId);
}
