package com.banking.loan.application;

import com.banking.loan.application.port.out.CustomerEligibilityPort;
import com.banking.loan.application.port.out.LoanLedgerPort;
import com.banking.loan.infrastructure.persistence.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoanWorkflowService {
    private static final BigDecimal MAX_APPLICATION= new BigDecimal("5000.00");
    private static final BigDecimal MAX_EXPOSURE= new BigDecimal("10000.00");
    private static final BigDecimal ANNUAL_RATE= new BigDecimal("0.120000");
    private static final String RULE_VERSION="prototype-v1";
    private final LoanApplicationJpaRepository applications;
    private final EligibilityDecisionJpaRepository decisions;
    private final LoanJpaRepository loans;
    private final InstallmentJpaRepository installments;
    private final RepaymentAttemptJpaRepository attempts;
    private final CustomerEligibilityPort customers;
    private final LoanLedgerPort ledger;

    public LoanWorkflowService(LoanApplicationJpaRepository applications,EligibilityDecisionJpaRepository decisions,LoanJpaRepository loans,
                              InstallmentJpaRepository installments,RepaymentAttemptJpaRepository attempts,
                              CustomerEligibilityPort customers,LoanLedgerPort ledger){
        this.applications=applications;this.decisions=decisions;this.loans=loans;this.installments=installments;this.attempts=attempts;this.customers=customers;this.ledger=ledger;
    }

    @Transactional
    public ApplicationView apply(UUID customerId,String key,BigDecimal amount,String rawCurrency,int termMonths){
        String currency=validateCurrency(rawCurrency);
        var previous=applications.findByCustomerIdAndIdempotencyKey(customerId,key);
        if(previous.isPresent()){
            LoanApplicationEntity existing=previous.get();
            if(existing.getRequestedAmount().compareTo(amount)!=0||!existing.getCurrency().equals(currency)||existing.getTermMonths()!=termMonths)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency-Key was already used with a different loan application");
            return applicationView(existing);
        }
        boolean active;
        try { active=customers.isActive(customerId); }
        catch(RuntimeException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Customer eligibility service is unavailable",ex);}
        String reason;
        if(!active) reason="Customer profile is not active";
        else if(amount==null||amount.scale()>2||amount.signum()<=0||amount.compareTo(MAX_APPLICATION)>0) reason="Requested amount must be between 0.01 and 5000.00";
        else if(termMonths<3||termMonths>36) reason="Term must be between 3 and 36 months";
        else if(loans.activePrincipalExposure(customerId).add(amount).compareTo(MAX_EXPOSURE)>0) reason="Active loan exposure would exceed 10000.00";
        else reason="Approved by prototype-v1 rules";
        boolean approved=reason.startsWith("Approved");
        LoanApplicationEntity app=applications.save(new LoanApplicationEntity(UUID.randomUUID(),customerId,amount,currency,termMonths,
                approved?"APPROVED":"REJECTED",Instant.now(),key));
        EligibilityDecisionEntity decision=decisions.save(new EligibilityDecisionEntity(UUID.randomUUID(),app.getId(),approved?"APPROVED":"REJECTED",reason,RULE_VERSION,Instant.now()));
        return applicationView(app,decision);
    }

    @Transactional(readOnly=true)
    public ApplicationView application(UUID customerId,UUID applicationId){return applicationView(ownedApplication(customerId,applicationId));}

    @Transactional(readOnly=true)
    public List<ApplicationView> applications(UUID customerId){return applications.findByCustomerIdOrderBySubmittedAtDesc(customerId).stream().map(this::applicationView).toList();}

    @Transactional
    public LoanView accept(UUID customerId,UUID applicationId){
        LoanApplicationEntity app=ownedApplication(customerId,applicationId);
        var existing=loans.findByApplicationId(app.getId());
        if(existing.isPresent()) return loanView(existing.get());
        if(!"APPROVED".equals(app.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only approved applications can be accepted");
        UUID loanId=stableId("loan:"+app.getId());
        UUID operationId=stableId("loan-disbursement:"+app.getId());
        UUID journal;
        try { journal=ledger.disburse(operationId,loanId,customerId,app.getRequestedAmount(),app.getCurrency()); }
        catch(LoanLedgerPort.LedgerUnavailable ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Ledger is unavailable; retry this acceptance",ex);}
        catch(RuntimeException ex){throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Ledger rejected loan disbursement: "+ex.getMessage(),ex);}
        LoanEntity loan=loans.save(new LoanEntity(loanId,app.getId(),customerId,app.getRequestedAmount(),app.getCurrency(),ANNUAL_RATE,
                app.getTermMonths(),"ACTIVE",Instant.now(),journal));
        createSchedule(loan);
        app.setStatus("ACCEPTED");
        return loanView(loan);
    }

    @Transactional(readOnly=true)
    public List<LoanView> loans(UUID customerId){return loans.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(this::loanView).toList();}

    @Transactional(readOnly=true)
    public List<InstallmentView> schedule(UUID customerId,UUID loanId){
        LoanEntity loan=ownedLoan(customerId,loanId);
        return installments.findByLoanIdOrderByInstallmentNumber(loan.getId()).stream().map(InstallmentView::from).toList();
    }

    @Transactional
    public RepaymentView repay(UUID customerId,UUID loanId,int installmentNumber){
        LoanEntity loan=ownedLoan(customerId,loanId);
        InstallmentEntity installment=installments.findByLoanIdAndInstallmentNumber(loanId,installmentNumber)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Installment not found"));
        if(installment.getDueDate().isAfter(LocalDate.now())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Installment is not due yet");
        return attemptRepayment(loan,installment);
    }

    @Transactional
    public int processDueInstallments(){
        List<InstallmentEntity> due=installments.findDueForUpdate(LocalDate.now());
        int processed=0;
        for(InstallmentEntity installment:due){
            loans.findById(installment.getLoanId()).ifPresent(loan->{
                try { attemptRepayment(loan,installment); }
                catch(RuntimeException ex){ /* Stable operation IDs make the next daily run safe. */ }
            });
            processed++;
        }
        return processed;
    }

    private RepaymentView attemptRepayment(LoanEntity loan,InstallmentEntity installment){
        if("PAID".equals(installment.getStatus())) return attempts.findByInstallmentId(installment.getId()).map(RepaymentView::from)
                .orElse(new RepaymentView(null,installment.getId(),"SUCCEEDED",null,null));
        UUID operationId=stableId("loan-repayment:"+installment.getId());
        RepaymentAttemptEntity attempt=attempts.findByInstallmentId(installment.getId()).orElseGet(()->attempts.save(new RepaymentAttemptEntity(
                UUID.randomUUID(),installment.getId(),operationId,installment.getTotalDue(),"PENDING",Instant.now(),null)));
        attempt.record("PENDING",Instant.now(),null);
        try {
            ledger.repay(operationId,loan.getId(),loan.getCustomerId(),installment.getPrincipalDue(),installment.getInterestDue(),loan.getCurrency());
            attempt.record("SUCCEEDED",Instant.now(),null);
            installment.setStatus("PAID");
            List<InstallmentEntity> all=installments.findByLoanIdOrderByInstallmentNumber(loan.getId());
            if(all.stream().allMatch(item->item.getId().equals(installment.getId())||"PAID".equals(item.getStatus()))) loan.setStatus("PAID");
        }catch(LoanLedgerPort.InsufficientFunds ex){
            attempt.record("FAILED",Instant.now(),ex.getMessage());
            installment.setStatus("OVERDUE");
        }catch(RuntimeException ex){
            attempt.record("FAILED",Instant.now(),"Ledger unavailable or rejected repayment");
            installment.setStatus("OVERDUE");
        }
        return RepaymentView.from(attempt);
    }

    private void createSchedule(LoanEntity loan){
        BigDecimal principal=loan.getPrincipalAmount().setScale(2,RoundingMode.UNNECESSARY);
        BigDecimal monthlyRate=loan.getAnnualRate().divide(BigDecimal.valueOf(12),MathContext.DECIMAL128);
        BigDecimal payment;
        if(monthlyRate.signum()==0) payment=principal.divide(BigDecimal.valueOf(loan.getTermMonths()),2,RoundingMode.HALF_UP);
        else {
            BigDecimal factor=BigDecimal.ONE.add(monthlyRate).pow(loan.getTermMonths(),MathContext.DECIMAL128);
            payment=principal.multiply(monthlyRate).multiply(factor).divide(factor.subtract(BigDecimal.ONE),2,RoundingMode.HALF_UP);
        }
        BigDecimal remaining=principal; LocalDate start=LocalDate.now();
        for(int number=1;number<=loan.getTermMonths();number++){
            BigDecimal interest=remaining.multiply(monthlyRate).setScale(2,RoundingMode.HALF_UP);
            BigDecimal principalDue=number==loan.getTermMonths()?remaining:payment.subtract(interest).setScale(2,RoundingMode.HALF_UP);
            if(principalDue.compareTo(remaining)>0) principalDue=remaining;
            BigDecimal total=principalDue.add(interest).setScale(2,RoundingMode.HALF_UP);
            installments.save(new InstallmentEntity(UUID.randomUUID(),loan.getId(),number,principalDue,interest,total,start.plusMonths(number),"SCHEDULED"));
            remaining=remaining.subtract(principalDue).setScale(2,RoundingMode.HALF_UP);
        }
    }

    private ApplicationView applicationView(LoanApplicationEntity app){
        EligibilityDecisionEntity decision=decisions.findByApplicationId(app.getId()).orElseThrow();
        return applicationView(app,decision);
    }
    private ApplicationView applicationView(LoanApplicationEntity app,EligibilityDecisionEntity decision){
        return new ApplicationView(app.getId(),app.getRequestedAmount(),app.getCurrency(),app.getTermMonths(),app.getStatus(),decision.getDecision(),decision.getReason(),decision.getRuleVersion(),app.getSubmittedAt());
    }
    private LoanApplicationEntity ownedApplication(UUID customerId,UUID appId){return applications.findByIdAndCustomerId(appId,customerId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Loan application not found"));}
    private LoanEntity ownedLoan(UUID customerId,UUID loanId){return loans.findByCustomerIdAndId(customerId,loanId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Loan not found"));}
    private LoanView loanView(LoanEntity loan){return new LoanView(loan.getId(),loan.getApplicationId(),loan.getPrincipalAmount(),loan.getCurrency(),loan.getAnnualRate(),loan.getTermMonths(),loan.getStatus(),loan.getLedgerTransactionId(),loan.getCreatedAt());}
    private String validateCurrency(String raw){String currency=raw==null?"":raw.toUpperCase(Locale.ROOT);try{if(!currency.matches("[A-Z]{3}")||Currency.getInstance(currency).getDefaultFractionDigits()!=2)throw new IllegalArgumentException();}catch(IllegalArgumentException ex){throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"V1 supports ISO currencies with two minor-unit digits only");}return currency;}
    private UUID stableId(String value){return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));}

    public record ApplicationView(UUID applicationId,BigDecimal requestedAmount,String currency,int termMonths,String status,String decision,String reason,String ruleVersion,Instant submittedAt){}
    public record LoanView(UUID loanId,UUID applicationId,BigDecimal principalAmount,String currency,BigDecimal annualRate,int termMonths,String status,UUID ledgerTransactionId,Instant createdAt){}
    public record InstallmentView(UUID installmentId,int installmentNumber,BigDecimal principalDue,BigDecimal interestDue,BigDecimal totalDue,LocalDate dueDate,String status){static InstallmentView from(InstallmentEntity i){return new InstallmentView(i.getId(),i.getInstallmentNumber(),i.getPrincipalDue(),i.getInterestDue(),i.getTotalDue(),i.getDueDate(),i.getStatus());}}
    public record RepaymentView(UUID operationId,UUID installmentId,String status,Instant attemptedAt,String failureReason){static RepaymentView from(RepaymentAttemptEntity a){return new RepaymentView(a.getOperationId(),a.getInstallmentId(),a.getStatus(),a.getAttemptedAt(),a.getFailureReason());}}
}
