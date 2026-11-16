package com.banking.customer.application;

import com.banking.customer.infrastructure.persistence.CustomerProfileEntity;
import com.banking.customer.infrastructure.persistence.CustomerProfileJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerProfileService {
    private final CustomerProfileJpaRepository profiles;
    public CustomerProfileService(CustomerProfileJpaRepository profiles) { this.profiles = profiles; }

    @Transactional(readOnly = true)
    public CustomerProfileEntity get(String subject) {
        return profiles.findByIdentitySubject(subject).orElseThrow(() -> new CustomerNotFound(subject));
    }

    @Transactional
    public CustomerProfileEntity onboard(String subject, String firstName, String lastName, String email) {
        UUID customerId;
        try { customerId=UUID.fromString(subject); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Authenticated subject must be a UUID in this prototype",ex); }
        return profiles.findByIdentitySubject(subject).orElseGet(() -> profiles.save(new CustomerProfileEntity(
                customerId, subject, firstName.trim(), lastName.trim(), email.trim().toLowerCase(), "ACTIVE", Instant.now())));
    }

    public static class CustomerNotFound extends RuntimeException {
        public CustomerNotFound(String subject) { super("Customer profile not found for authenticated identity " + subject); }
    }
}
