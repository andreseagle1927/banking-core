package com.banking.customer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "customer_profiles")
public class CustomerProfileEntity {
    @Id private UUID id;
    @Column(name = "identity_subject", nullable = false, unique = true, length = 128) private String identitySubject;
    @Column(name = "first_name", nullable = false, length = 120) private String firstName;
    @Column(name = "last_name", nullable = false, length = 120) private String lastName;
    @Column(nullable = false, length = 254) private String email;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected CustomerProfileEntity() { }
}
