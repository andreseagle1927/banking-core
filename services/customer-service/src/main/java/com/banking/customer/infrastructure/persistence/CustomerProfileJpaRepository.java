package com.banking.customer.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileJpaRepository extends JpaRepository<CustomerProfileEntity, UUID> {
    Optional<CustomerProfileEntity> findByIdentitySubject(String identitySubject);
}
