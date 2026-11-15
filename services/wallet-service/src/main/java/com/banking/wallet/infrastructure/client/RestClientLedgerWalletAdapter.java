package com.banking.wallet.infrastructure.client;

import com.banking.wallet.application.port.out.LedgerWalletPort;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientLedgerWalletAdapter implements LedgerWalletPort {
    private final RestClient ledger;
    private final OAuth2AuthorizedClientManager clients;
    public RestClientLedgerWalletAdapter(RestClient.Builder builder, @Value("${LEDGER_URL:http://localhost:8085}") String url,
                                         OAuth2AuthorizedClientManager clients) {
        this.ledger=builder.baseUrl(url).build(); this.clients=clients;
    }
    private String token() {
        var client=clients.authorize(OAuth2AuthorizeRequest.withClientRegistrationId("ledger-service").principal("wallet-service").build());
        if (client==null) throw new IllegalStateException("Ledger service credentials were not authorized");
        return client.getAccessToken().getTokenValue();
    }
    @Override public void createAccount(UUID walletId, String currency) {
        ledger.post().uri("/internal/ledger/accounts").headers(h->h.setBearerAuth(token()))
                .body(Map.of("walletId",walletId,"currency",currency)).retrieve().toBodilessEntity();
    }
    @Override public void setAccountStatus(UUID walletId, String currency, String status) {
        ledger.patch().uri("/internal/ledger/accounts/{walletId}/{currency}/status",walletId,currency)
                .headers(h->h.setBearerAuth(token())).body(Map.of("status",status)).retrieve().toBodilessEntity();
    }
    @Override public long balance(UUID walletId, String currency) {
        Map<?,?> result=ledger.get().uri("/internal/ledger/accounts/{walletId}/balances/{currency}",walletId,currency)
                .headers(h->h.setBearerAuth(token())).retrieve().body(Map.class);
        if (result==null || result.get("balanceMinor")==null) throw new IllegalStateException("Ledger returned no balance");
        return ((Number)result.get("balanceMinor")).longValue();
    }
}
