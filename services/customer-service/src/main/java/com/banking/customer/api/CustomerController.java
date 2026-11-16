package com.banking.customer.api;

import com.banking.customer.application.CustomerProfileService;
import com.banking.customer.application.CustomerProfileService.CustomerNotFound;
import com.banking.customer.infrastructure.persistence.CustomerProfileEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    private final CustomerProfileService customers;
    public CustomerController(CustomerProfileService customers) { this.customers = customers; }

    @GetMapping("/me")
    public CustomerResponse me(JwtAuthenticationToken principal) {
        try { return CustomerResponse.from(customers.get(principal.getToken().getSubject())); }
        catch (CustomerNotFound ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage()); }
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse onboard(@Valid @RequestBody OnboardRequest request, JwtAuthenticationToken principal) {
        return CustomerResponse.from(customers.onboard(principal.getToken().getSubject(), request.firstName(), request.lastName(), request.email()));
    }

    @GetMapping("/internal/{customerId}")
    @PreAuthorize("hasAnyRole('wallet-provisioner','loan-underwriter','notification-reader')")
    public CustomerResponse internal(@PathVariable UUID customerId) {
        try { return CustomerResponse.from(customers.get(customerId.toString())); }
        catch (CustomerNotFound ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"); }
    }

    public record OnboardRequest(@NotBlank @Size(max=120) String firstName,
                                 @NotBlank @Size(max=120) String lastName,
                                 @NotBlank @Email @Size(max=254) String email) { }
    public record CustomerResponse(UUID customerId, String firstName, String lastName, String email,
                                   String status, Instant createdAt) {
        static CustomerResponse from(CustomerProfileEntity entity) {
            return new CustomerResponse(entity.getId(), entity.getFirstName(), entity.getLastName(), entity.getEmail(), entity.getStatus(), entity.getCreatedAt());
        }
    }
}
