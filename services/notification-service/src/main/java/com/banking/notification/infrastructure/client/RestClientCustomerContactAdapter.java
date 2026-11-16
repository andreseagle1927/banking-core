package com.banking.notification.infrastructure.client;
import com.banking.notification.application.port.out.CustomerContactPort;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
@Component
public class RestClientCustomerContactAdapter implements CustomerContactPort {
    private final RestClient customers;private final OAuth2AuthorizedClientManager clients;
    public RestClientCustomerContactAdapter(RestClient.Builder builder,@Value("${CUSTOMER_SERVICE_URL:http://localhost:8082}") String url,OAuth2AuthorizedClientManager clients){this.customers=builder.baseUrl(url).build();this.clients=clients;}
    @Override public Optional<String> emailFor(UUID customerId){
        try{
            var client=clients.authorize(OAuth2AuthorizeRequest.withClientRegistrationId("notification-service").principal("notification-service").build());
            if(client==null)throw new IllegalStateException("Notification service identity was not authorized");
            Map<?,?> profile=customers.get().uri("/api/v1/customers/internal/{id}",customerId).headers(h->h.setBearerAuth(client.getAccessToken().getTokenValue())).retrieve().body(Map.class);
            return profile==null||profile.get("email")==null?Optional.empty():Optional.of(profile.get("email").toString());
        }catch(HttpClientErrorException.NotFound ex){return Optional.empty();}
    }
}
