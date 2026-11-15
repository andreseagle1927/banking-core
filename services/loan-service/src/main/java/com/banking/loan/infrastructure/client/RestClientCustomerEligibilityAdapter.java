package com.banking.loan.infrastructure.client;
import com.banking.loan.application.port.out.CustomerEligibilityPort;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
@Component
public class RestClientCustomerEligibilityAdapter implements CustomerEligibilityPort {
    private final RestClient customers; private final LoanServiceIdentity identity;
    public RestClientCustomerEligibilityAdapter(RestClient.Builder builder,@Value("${CUSTOMER_SERVICE_URL:http://localhost:8082}") String url,LoanServiceIdentity identity){this.customers=builder.baseUrl(url).build();this.identity=identity;}
    @Override public boolean isActive(UUID customerId){
        try { Map<?,?> profile=customers.get().uri("/api/v1/customers/internal/{id}",customerId).headers(h->h.setBearerAuth(identity.token())).retrieve().body(Map.class);
            return profile!=null&&"ACTIVE".equals(profile.get("status"));
        } catch(HttpClientErrorException.NotFound ex){return false;}
    }
}
