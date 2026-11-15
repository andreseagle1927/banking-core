package com.banking.loan.infrastructure.client;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
@Component
public class LoanServiceIdentity {
    private final OAuth2AuthorizedClientManager clients;
    public LoanServiceIdentity(OAuth2AuthorizedClientManager clients){this.clients=clients;}
    public String token(){var client=clients.authorize(OAuth2AuthorizeRequest.withClientRegistrationId("loan-service").principal("loan-service").build());
        if(client==null)throw new IllegalStateException("Loan service credentials were not authorized");return client.getAccessToken().getTokenValue();}
}
