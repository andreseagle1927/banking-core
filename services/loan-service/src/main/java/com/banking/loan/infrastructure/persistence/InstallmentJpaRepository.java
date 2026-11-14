package com.banking.loan.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentJpaRepository extends JpaRepository<InstallmentEntity, UUID> {
    List<InstallmentEntity> findByLoanIdOrderByInstallmentNumber(UUID loanId);
}
