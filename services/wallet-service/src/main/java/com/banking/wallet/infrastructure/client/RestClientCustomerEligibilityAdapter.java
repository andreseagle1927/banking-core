package com.banking.wallet.infrastructure.client;

import com.banking.wallet.application.port.out.CustomerEligibilityPort;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class RestClientCustomerEligibilityAdapter implements CustomerEligibilityPort {
    private final RestClient customers; private final OAuth2AuthorizedClientManager clients;
    public RestClientCustomerEligibilityAdapter(RestClient.Builder builder,@Value("${CUSTOMER_SERVICE_URL:http://localhost:8082}") String url,OAuth2AuthorizedClientManager clients){this.customers=builder.baseUrl(url).build();this.clients=clients;}
    @Override public boolean isActive(UUID customerId){
        try {
            var client=clients.authorize(OAuth2AuthorizeRequest.withClientRegistrationId("ledger-service").principal("wallet-service").build());
            if(client==null)throw new IllegalStateException("Wallet service identity was not authorized");
            Map<?,?> profile=customers.get().uri("/api/v1/customers/internal/{id}",customerId).headers(h->h.setBearerAuth(client.getAccessToken().getTokenValue())).retrieve().body(Map.class);
            return profile!=null&&"ACTIVE".equals(profile.get("status"));
        } catch(HttpClientErrorException.NotFound ex){return false;}
    }
}
