package com.banking.loan.infrastructure.client;
import com.banking.loan.application.port.out.LoanLedgerPort;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
@Component
public class RestClientLoanLedgerAdapter implements LoanLedgerPort {
    private final RestClient ledger; private final LoanServiceIdentity identity;
    public RestClientLoanLedgerAdapter(RestClient.Builder builder,@Value("${LEDGER_URL:http://localhost:8085}") String url,LoanServiceIdentity identity){this.ledger=builder.baseUrl(url).build();this.identity=identity;}
    @Override public UUID disburse(UUID operationId,UUID loanId,UUID walletId,BigDecimal amount,String currency){return post("/internal/ledger/loan-disbursements",Map.of("operationId",operationId,"loanId",loanId,"walletId",walletId,"amount",amount,"currency",currency),false);}
    @Override public UUID repay(UUID operationId,UUID loanId,UUID walletId,BigDecimal principal,BigDecimal interest,String currency){return post("/internal/ledger/loan-repayments",Map.of("operationId",operationId,"loanId",loanId,"walletId",walletId,"principal",principal,"interest",interest,"currency",currency),true);}
    private UUID post(String path,Map<String,Object> request,boolean repayment){
        try { Map<?,?> result=ledger.post().uri(path).headers(h->h.setBearerAuth(identity.token())).body(request).retrieve().body(Map.class);
            if(result==null||result.get("ledgerTransactionId")==null)throw new IllegalStateException("Ledger returned no journal identifier");
            return UUID.fromString(result.get("ledgerTransactionId").toString());
        } catch(HttpClientErrorException.UnprocessableEntity ex){if(repayment&&ex.getResponseBodyAsString().contains("Insufficient"))throw new InsufficientFunds("Insufficient wallet balance");throw ex;}
        catch(ResourceAccessException ex){throw new LedgerUnavailable("Ledger is temporarily unavailable",ex);}
    }
}
