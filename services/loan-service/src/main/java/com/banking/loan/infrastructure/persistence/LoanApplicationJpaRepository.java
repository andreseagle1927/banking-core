package com.banking.loan.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationJpaRepository extends JpaRepository<LoanApplicationEntity, UUID> {
    List<LoanApplicationEntity> findByCustomerIdOrderBySubmittedAtDesc(UUID customerId);
}
