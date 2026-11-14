package com.banking.loan.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanJpaRepository extends JpaRepository<LoanEntity, UUID> {
    List<LoanEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
