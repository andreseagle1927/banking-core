package com.banking.wallet.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.banking.wallet.application.port.out.CustomerEligibilityPort;
import com.banking.wallet.application.port.out.LedgerWalletPort;
import com.banking.wallet.infrastructure.persistence.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletApplicationServiceTest {
    @Mock WalletJpaRepository wallets;
    @Mock WalletCurrencyJpaRepository currencies;
    @Mock LedgerWalletPort ledger;
    @Mock CustomerEligibilityPort customers;
    @InjectMocks WalletApplicationService service;

    @Test void opensOneWalletWithTheAuthenticatedCustomerId(){
        UUID customerId=UUID.randomUUID();
        when(customers.isActive(customerId)).thenReturn(true);
        when(wallets.findByCustomerId(customerId)).thenReturn(Optional.empty());
        when(wallets.save(any())).thenAnswer(call->call.getArgument(0));
        WalletEntity wallet=service.open(customerId);
        assertEquals(customerId,wallet.getId());
        assertEquals(customerId,wallet.getCustomerId());
        verify(wallets).save(any(WalletEntity.class));
    }

    @Test void enablingCurrencyCreatesTheLedgerAccountBeforeSavingThePocket(){
        UUID customerId=UUID.randomUUID();
        WalletEntity wallet=new WalletEntity(customerId,customerId,"ACTIVE",Instant.now());
        when(customers.isActive(customerId)).thenReturn(true);
        when(wallets.findByCustomerId(customerId)).thenReturn(Optional.of(wallet));
        when(currencies.findByWalletIdAndCurrency(customerId,"EUR")).thenReturn(Optional.empty());
        when(currencies.save(any())).thenAnswer(call->call.getArgument(0));
        WalletCurrencyEntity pocket=service.enableCurrency(customerId,"eur");
        assertEquals("EUR",pocket.getCurrency());
        assertEquals("ACTIVE",pocket.getStatus());
        verify(ledger).createAccount(customerId,"EUR");
        verify(currencies).save(any(WalletCurrencyEntity.class));
    }

    @Test void cannotCreateWalletWithoutActiveCustomerProfile(){
        UUID customerId=UUID.randomUUID();when(customers.isActive(customerId)).thenReturn(false);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.open(customerId));
        verify(wallets,never()).save(any());
    }
}
