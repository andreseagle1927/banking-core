package com.banking.ledger.api;

import com.banking.ledger.application.LedgerPostingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@RestController
@RequestMapping("/internal/ledger")
public class LedgerController {
    private final LedgerPostingService ledger;
    public LedgerController(LedgerPostingService ledger) { this.ledger = ledger; }

    @PostMapping("/accounts")
    @PreAuthorize("hasRole('platform-operator')")
    public Map<String, Object> createAccount(@Valid @RequestBody AccountRequest request) {
        try { ledger.createAccount(request.walletId(), request.currency()); }
        catch (LedgerPostingService.InvalidAccount ex) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()); }
        return Map.of("walletId", request.walletId(), "currency", request.currency(), "status", "ACTIVE");
    }

    @PostMapping("/funding")
    @PreAuthorize("hasRole('platform-operator')")
    public Map<String, Object> fund(@Valid @RequestBody FundingRequest request) {
        try { return ledger.fund(request.operationId(), request.walletId(), request.amount(), request.currency()); }
        catch (LedgerPostingService.InvalidAmount | LedgerPostingService.InvalidAccount ex) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()); }
        catch (LedgerPostingService.IdempotencyConflict ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage()); }
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasRole('ledger-poster')")
    public Map<String, Object> transfer(@Valid @RequestBody TransferRequest request) {
        try { return ledger.transfer(request.operationId(), request.senderWalletId(), request.recipientWalletId(), request.amount(), request.currency()); }
        catch (LedgerPostingService.InsufficientFunds ex) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()); }
        catch (LedgerPostingService.InvalidAccount ex) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()); }
        catch (LedgerPostingService.IdempotencyConflict ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage()); }
        catch (LedgerPostingService.InvalidAmount ex) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()); }
    }

    @GetMapping("/accounts/{walletId}/balances/{currency}")
    public Map<String, Object> balance(@PathVariable UUID walletId, @PathVariable String currency, JwtAuthenticationToken principal) {
        boolean operator = principal.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_platform-operator"));
        if (!operator && !principal.getToken().getSubject().equals(walletId.toString()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        return ledger.balance(walletId, currency.toUpperCase(Locale.ROOT));
    }

    public record AccountRequest(@NotNull UUID walletId, @NotBlank @Pattern(regexp="[A-Z]{3}") String currency) { }
    public record FundingRequest(@NotNull UUID operationId, @NotNull UUID walletId, @NotNull @Positive @Digits(integer=16, fraction=2) BigDecimal amount, @NotBlank @Pattern(regexp="[A-Z]{3}") String currency) { }
    public record TransferRequest(@NotNull UUID operationId, @NotNull UUID senderWalletId, @NotNull UUID recipientWalletId, @NotNull @Positive @Digits(integer=16, fraction=2) BigDecimal amount, @NotBlank @Pattern(regexp="[A-Z]{3}") String currency) { }
}
