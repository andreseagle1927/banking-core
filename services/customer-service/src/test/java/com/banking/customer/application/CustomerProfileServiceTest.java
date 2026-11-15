package com.banking.customer.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.banking.customer.infrastructure.persistence.CustomerProfileEntity;
import com.banking.customer.infrastructure.persistence.CustomerProfileJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {
    @Mock CustomerProfileJpaRepository profiles;
    @InjectMocks CustomerProfileService service;

    @Test void onboardsOnceUsingTheAuthenticatedSubjectAsCustomerId(){
        UUID subject=UUID.randomUUID();
        when(profiles.findByIdentitySubject(subject.toString())).thenReturn(Optional.empty());
        when(profiles.save(any())).thenAnswer(call->call.getArgument(0));
        CustomerProfileEntity profile=service.onboard(subject.toString()," Alice "," Demo ","ALICE@example.local");
        assertEquals(subject,profile.getId());
        assertEquals(subject.toString(),profile.getIdentitySubject());
        assertEquals("Alice",profile.getFirstName());
        assertEquals("alice@example.local",profile.getEmail());
        assertEquals("ACTIVE",profile.getStatus());
    }

    @Test void rejectsNonUuidIdentitySubjects(){
        assertThrows(IllegalArgumentException.class,()->service.onboard("not-a-uuid","A","B","a@example.local"));
        verifyNoInteractions(profiles);
    }
}
