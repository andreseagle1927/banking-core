package com.banking.ledger.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.ledger.application.LedgerPostingService;
import com.banking.platform.security.ResourceServerSecurity;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LedgerController.class)
@Import(ResourceServerSecurity.class)
class LedgerControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean LedgerPostingService ledger;

    @Test
    void requiresAuthenticationForLedgerOperations() throws Exception {
        mvc.perform(post("/internal/ledger/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"walletId\":\"11111111-1111-4111-8111-111111111111\",\"currency\":\"USD\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(ledger);
    }

    @Test
    void customersCannotProvisionAccountsOrPostLedgerTransfers() throws Exception {
        var customer = jwt().jwt(token -> token.subject("11111111-1111-4111-8111-111111111111"))
                .authorities(new SimpleGrantedAuthority("ROLE_customer"));

        mvc.perform(post("/internal/ledger/accounts").with(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"walletId\":\"22222222-2222-4222-8222-222222222222\",\"currency\":\"USD\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/internal/ledger/transfers").with(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operationId\":\"33333333-3333-4333-8333-333333333333\",\"senderWalletId\":\"22222222-2222-4222-8222-222222222222\",\"recipientWalletId\":\"44444444-4444-4444-8444-444444444444\",\"amount\":\"1.00\",\"currency\":\"USD\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ledger);
    }

    @Test
    void onlyTransferServiceRoleCanPostMoney() throws Exception {
        UUID operationId = UUID.fromString("33333333-3333-4333-8333-333333333333");
        UUID sender = UUID.fromString("11111111-1111-4111-8111-111111111111");
        UUID recipient = UUID.fromString("22222222-2222-4222-8222-222222222222");
        when(ledger.transfer(operationId, sender, recipient, new java.math.BigDecimal("1.00"), "USD"))
                .thenReturn(Map.of("status", "POSTED"));

        mvc.perform(post("/internal/ledger/transfers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ledger-poster")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operationId\":\"33333333-3333-4333-8333-333333333333\",\"senderWalletId\":\"11111111-1111-4111-8111-111111111111\",\"recipientWalletId\":\"22222222-2222-4222-8222-222222222222\",\"amount\":\"1.00\",\"currency\":\"USD\"}"))
                .andExpect(status().isOk());

        verify(ledger).transfer(operationId, sender, recipient, new java.math.BigDecimal("1.00"), "USD");
    }
}
