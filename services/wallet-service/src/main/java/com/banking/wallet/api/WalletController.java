package com.banking.wallet.api;

import com.banking.wallet.application.WalletApplicationService;
import com.banking.wallet.application.WalletApplicationService.CurrencyBalance;
import com.banking.wallet.infrastructure.persistence.WalletCurrencyEntity;
import com.banking.wallet.infrastructure.persistence.WalletEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {
    private final WalletApplicationService wallets;
    public WalletController(WalletApplicationService wallets) { this.wallets=wallets; }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse open(JwtAuthenticationToken principal) { return WalletResponse.from(wallets.open(subject(principal))); }

    @GetMapping("/me")
    public WalletResponse me(JwtAuthenticationToken principal) {
        WalletEntity wallet=wallets.get(subject(principal));
        return WalletResponse.from(wallet,wallets.listCurrencies(wallet.getCustomerId()));
    }

    @PutMapping("/me/currencies/{currency}")
    public CurrencyResponse enableCurrency(@PathVariable String currency,JwtAuthenticationToken principal) {
        return CurrencyResponse.from(wallets.enableCurrency(subject(principal),currency));
    }

    @GetMapping("/me/balances")
    public List<CurrencyBalance> balances(JwtAuthenticationToken principal) { return wallets.balances(subject(principal)); }

    @PatchMapping("/me/status")
    public WalletResponse setStatus(@Valid @RequestBody StatusRequest request,JwtAuthenticationToken principal) {
        WalletEntity wallet=wallets.changeStatus(subject(principal),request.status());
        return WalletResponse.from(wallet,wallets.listCurrencies(wallet.getCustomerId()));
    }

    private UUID subject(JwtAuthenticationToken principal) {
        try { return UUID.fromString(principal.getToken().getSubject()); }
        catch (IllegalArgumentException ex) { throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authenticated subject is not a UUID customer identity"); }
    }
    public record StatusRequest(@NotBlank @Pattern(regexp="ACTIVE|BLOCKED|CLOSED") String status) { }
    public record WalletResponse(UUID walletId,UUID customerId,String status,Instant createdAt,List<CurrencyResponse> currencies) {
        static WalletResponse from(WalletEntity entity) { return from(entity,List.of()); }
        static WalletResponse from(WalletEntity entity,List<WalletCurrencyEntity> currencies) {
            return new WalletResponse(entity.getId(),entity.getCustomerId(),entity.getStatus(),entity.getCreatedAt(),currencies.stream().map(CurrencyResponse::from).toList());
        }
    }
    public record CurrencyResponse(String currency,String status,Instant enabledAt) {
        static CurrencyResponse from(WalletCurrencyEntity entity) { return new CurrencyResponse(entity.getCurrency(),entity.getStatus(),entity.getCreatedAt()); }
    }
}
