package com.banking.loan.api;

import com.banking.loan.application.LoanWorkflowService;
import com.banking.loan.application.LoanWorkflowService.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {
    private final LoanWorkflowService loans;
    public LoanController(LoanWorkflowService loans){this.loans=loans;}

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationView apply(@RequestHeader("Idempotency-Key") @NotBlank @Size(max=128) String key,
                                 @Valid @RequestBody ApplicationRequest request,JwtAuthenticationToken principal){
        return loans.apply(subject(principal),key,request.amount(),request.currency(),request.termMonths());
    }
    @GetMapping("/applications") public List<ApplicationView> applications(JwtAuthenticationToken principal){return loans.applications(subject(principal));}
    @GetMapping("/applications/{applicationId}") public ApplicationView application(@PathVariable UUID applicationId,JwtAuthenticationToken principal){return loans.application(subject(principal),applicationId);}
    @PostMapping("/applications/{applicationId}/accept") public LoanView accept(@PathVariable UUID applicationId,JwtAuthenticationToken principal){return loans.accept(subject(principal),applicationId);}
    @GetMapping public List<LoanView> list(JwtAuthenticationToken principal){return loans.loans(subject(principal));}
    @GetMapping("/{loanId}/schedule") public List<InstallmentView> schedule(@PathVariable UUID loanId,JwtAuthenticationToken principal){return loans.schedule(subject(principal),loanId);}
    @PostMapping("/{loanId}/installments/{number}/repay") public RepaymentView repay(@PathVariable UUID loanId,@PathVariable @Min(1) int number,JwtAuthenticationToken principal){return loans.repay(subject(principal),loanId,number);}

    @PostMapping("/internal/repayments/run")
    @PreAuthorize("hasRole('platform-operator')")
    public Map<String,Integer> runDueRepayments(){return Map.of("processed",loans.processDueInstallments());}

    private UUID subject(JwtAuthenticationToken principal){
        try{return UUID.fromString(principal.getToken().getSubject());}
        catch(IllegalArgumentException ex){throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authenticated subject is not a UUID customer identity");}
    }
    public record ApplicationRequest(@NotNull @Positive @Digits(integer=16,fraction=2) BigDecimal amount,
                                     @NotBlank @Pattern(regexp="[A-Za-z]{3}") String currency,
                                     @Min(3) @Max(36) int termMonths){}
}
