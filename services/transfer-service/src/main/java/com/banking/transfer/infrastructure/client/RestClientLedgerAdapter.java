package com.banking.transfer.infrastructure.client;

import com.banking.transfer.application.exception.LedgerCallException;
import com.banking.transfer.application.exception.LedgerUnavailableException;
import com.banking.transfer.application.port.out.LedgerPostingPort;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestClientLedgerAdapter implements LedgerPostingPort {
    private final RestClient ledger;
    private final OAuth2AuthorizedClientManager authorizedClients;
    public RestClientLedgerAdapter(RestClient.Builder builder, @Value("${LEDGER_URL:http://localhost:8085}") String ledgerUrl,
                                  OAuth2AuthorizedClientManager authorizedClients) {
        this.ledger = builder.baseUrl(ledgerUrl).build();
        this.authorizedClients = authorizedClients;
    }

    @Override
    public UUID postTransfer(UUID operationId, UUID senderWalletId, UUID recipientWalletId,
                             BigDecimal amount, String currency) {
        try {
            OAuth2AuthorizedClient client = authorizedClients.authorize(OAuth2AuthorizeRequest
                    .withClientRegistrationId("ledger-service").principal("transfer-service").build());
            if (client == null) throw new IllegalStateException("Keycloak did not authorize the Ledger service client");
            Map<?, ?> result = ledger.post().uri("/internal/ledger/transfers")
                    .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                    .body(Map.of("operationId", operationId, "senderWalletId", senderWalletId,
                            "recipientWalletId", recipientWalletId, "amount", amount, "currency", currency))
                    .retrieve().body(Map.class);
            if (result == null || result.get("ledgerTransactionId") == null)
                throw new LedgerCallException(502, "Ledger returned no posting identifier");
            return UUID.fromString(result.get("ledgerTransactionId").toString());
        } catch (RestClientResponseException ex) {
            throw new LedgerCallException(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            throw new LedgerUnavailableException(ex);
        } catch (OAuth2AuthorizationException ex) {
            throw new LedgerUnavailableException(ex);
        }
    }
}
