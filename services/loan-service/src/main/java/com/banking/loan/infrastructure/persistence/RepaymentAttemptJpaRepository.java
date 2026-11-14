package com.banking.loan.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentAttemptJpaRepository extends JpaRepository<RepaymentAttemptEntity, UUID> {
    Optional<RepaymentAttemptEntity> findByOperationId(UUID operationId);
}
