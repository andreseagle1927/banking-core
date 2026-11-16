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
    @PreAuthorize("hasAnyRole('platform-operator','wallet-provisioner')")
    public Map<String, Object> createAccount(@Valid @RequestBody AccountRequest request) {
        try { ledger.createAccount(request.walletId(), request.currency()); }
        catch (LedgerPostingService.InvalidAccount ex) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()); }
        return Map.of("walletId", request.walletId(), "currency", request.currency(), "status", "ACTIVE");
    }

    @PatchMapping("/accounts/{walletId}/{currency}/status")
    @PreAuthorize("hasRole('wallet-provisioner')")
    public Map<String,Object> setAccountStatus(@PathVariable UUID walletId,@PathVariable String currency,@Valid @RequestBody AccountStatusRequest request) {
        try { ledger.setWalletAccountStatus(walletId,currency.toUpperCase(Locale.ROOT),request.status()); }
        catch (LedgerPostingService.InvalidAccount ex) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,ex.getMessage()); }
        return Map.of("walletId",walletId,"currency",currency.toUpperCase(Locale.ROOT),"status",request.status());
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

    @PostMapping("/loan-disbursements")
    @PreAuthorize("hasRole('loan-poster')")
    public Map<String,Object> disburseLoan(@Valid @RequestBody LoanDisbursementRequest request) {
        try { return ledger.disburseLoan(request.operationId(),request.loanId(),request.walletId(),request.amount(),request.currency()); }
        catch (LedgerPostingService.InvalidAmount | LedgerPostingService.InvalidAccount ex) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,ex.getMessage()); }
        catch (LedgerPostingService.IdempotencyConflict ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage()); }
    }

    @PostMapping("/loan-repayments")
    @PreAuthorize("hasRole('loan-poster')")
    public Map<String,Object> repayLoan(@Valid @RequestBody LoanRepaymentRequest request) {
        try { return ledger.repayLoan(request.operationId(),request.loanId(),request.walletId(),request.principal(),request.interest(),request.currency()); }
        catch (LedgerPostingService.InsufficientFunds | LedgerPostingService.InvalidAccount | LedgerPostingService.InvalidAmount ex) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,ex.getMessage()); }
        catch (LedgerPostingService.IdempotencyConflict ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,ex.getMessage()); }
    }

    @GetMapping("/accounts/{walletId}/balances/{currency}")
    public Map<String, Object> balance(@PathVariable UUID walletId, @PathVariable String currency, JwtAuthenticationToken principal) {
        boolean operator = principal.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_platform-operator"));
        boolean walletReader = principal.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_wallet-reader"));
        if (!operator && !walletReader && !principal.getToken().getSubject().equals(walletId.toString()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        return ledger.balance(walletId, currency.toUpperCase(Locale.ROOT));
    }

    public record AccountRequest(@NotNull UUID walletId, @NotBlank @Pattern(regexp="[A-Z]{3}") String currency) { }
    public record FundingRequest(@NotNull UUID operationId, @NotNull UUID walletId, @NotNull @Positive @Digits(integer=16, fraction=2) BigDecimal amount, @NotBlank @Pattern(regexp="[A-Z]{3}") String currency) { }
    public record TransferRequest(@NotNull UUID operationId, @NotNull UUID senderWalletId, @NotNull UUID recipientWalletId, @NotNull @Positive @Digits(integer=16, fraction=2) BigDecimal amount, @NotBlank @Pattern(regexp="[A-Z]{3}") String currency) { }
    public record AccountStatusRequest(@NotBlank @Pattern(regexp="ACTIVE|BLOCKED|CLOSED") String status) { }
    public record LoanDisbursementRequest(@NotNull UUID operationId,@NotNull UUID loanId,@NotNull UUID walletId,@NotNull @Positive @Digits(integer=16,fraction=2) BigDecimal amount,@NotBlank @Pattern(regexp="[A-Z]{3}") String currency) { }
    public record LoanRepaymentRequest(@NotNull UUID operationId,@NotNull UUID loanId,@NotNull UUID walletId,@NotNull @Positive @Digits(integer=16,fraction=2) BigDecimal principal,@NotNull @Digits(integer=16,fraction=2) BigDecimal interest,@NotBlank @Pattern(regexp="[A-Z]{3}") String currency) { }
}
