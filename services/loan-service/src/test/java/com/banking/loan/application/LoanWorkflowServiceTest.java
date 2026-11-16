package com.banking.loan.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.banking.loan.application.port.out.CustomerEligibilityPort;
import com.banking.loan.application.port.out.LoanLedgerPort;
import com.banking.loan.infrastructure.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanWorkflowServiceTest {
    @Mock LoanApplicationJpaRepository applications;
    @Mock EligibilityDecisionJpaRepository decisions;
    @Mock LoanJpaRepository loans;
    @Mock InstallmentJpaRepository installments;
    @Mock RepaymentAttemptJpaRepository attempts;
    @Mock CustomerEligibilityPort customers;
    @Mock LoanLedgerPort ledger;
    @InjectMocks LoanWorkflowService service;
    private final UUID customerId=UUID.randomUUID();

    @BeforeEach void defaults(){
        lenient().when(customers.isActive(customerId)).thenReturn(true);
        lenient().when(loans.activePrincipalExposure(customerId)).thenReturn(BigDecimal.ZERO);
        lenient().when(applications.save(any())).thenAnswer(call->call.getArgument(0));
        lenient().when(decisions.save(any())).thenAnswer(call->call.getArgument(0));
    }

    @Test void approvesAndPersistsAnEligibleApplication(){
        when(applications.findByCustomerIdAndIdempotencyKey(customerId,"application-001")).thenReturn(Optional.empty());
        var result=service.apply(customerId,"application-001",new BigDecimal("1200.00"),"usd",12);
        assertEquals("APPROVED",result.decision());
        assertEquals("USD",result.currency());
        assertEquals("prototype-v1",result.ruleVersion());
        verify(decisions).save(argThat(decision->"APPROVED".equals(decision.getDecision())));
    }

    @Test void disbursesApprovedLoanOnceAndCreatesRoundedFixedSchedule(){
        UUID applicationId=UUID.randomUUID(),journalId=UUID.randomUUID();
        LoanApplicationEntity application=new LoanApplicationEntity(applicationId,customerId,new BigDecimal("1200.00"),"USD",12,"APPROVED",Instant.now(),"accept-key");
        when(applications.findByIdAndCustomerId(applicationId,customerId)).thenReturn(Optional.of(application));
        when(loans.findByApplicationId(applicationId)).thenReturn(Optional.empty());
        when(ledger.disburse(any(),any(),eq(customerId),eq(new BigDecimal("1200.00")),eq("USD"))).thenReturn(journalId);
        when(loans.save(any())).thenAnswer(call->call.getArgument(0));
        when(installments.save(any())).thenAnswer(call->call.getArgument(0));

        var loan=service.accept(customerId,applicationId);
        assertEquals("ACTIVE",loan.status());
        assertEquals(journalId,loan.ledgerTransactionId());
        assertEquals("ACCEPTED",application.getStatus());
        ArgumentCaptor<InstallmentEntity> schedule=ArgumentCaptor.forClass(InstallmentEntity.class);
        verify(installments,times(12)).save(schedule.capture());
        BigDecimal totalPrincipal=schedule.getAllValues().stream().map(InstallmentEntity::getPrincipalDue).reduce(BigDecimal.ZERO,BigDecimal::add);
        assertEquals(0,new BigDecimal("1200.00").compareTo(totalPrincipal));
        assertTrue(schedule.getAllValues().stream().allMatch(item->item.getTotalDue().compareTo(BigDecimal.ZERO)>0));
        verify(ledger,times(1)).disburse(any(),any(),eq(customerId),any(),eq("USD"));
    }

    @Test void recordsInsufficientFundsWithoutMarkingInstallmentPaid(){
        UUID loanId=UUID.randomUUID();
        LoanEntity loan=new LoanEntity(loanId,UUID.randomUUID(),customerId,new BigDecimal("100.00"),"USD",new BigDecimal("0.120000"),12,"ACTIVE",Instant.now(),UUID.randomUUID());
        InstallmentEntity installment=new InstallmentEntity(UUID.randomUUID(),loanId,1,new BigDecimal("8.00"),new BigDecimal("1.00"),new BigDecimal("9.00"),LocalDate.now().minusDays(1),"SCHEDULED");
        when(loans.findByCustomerIdAndId(customerId,loanId)).thenReturn(Optional.of(loan));
        when(installments.findByLoanIdAndInstallmentNumber(loanId,1)).thenReturn(Optional.of(installment));
        when(attempts.findByInstallmentId(installment.getId())).thenReturn(Optional.empty());
        when(attempts.save(any())).thenAnswer(call->call.getArgument(0));
        doThrow(new LoanLedgerPort.InsufficientFunds("Insufficient wallet balance")).when(ledger).repay(any(),eq(loanId),eq(customerId),any(),any(),eq("USD"));

        var result=service.repay(customerId,loanId,1);
        assertEquals("FAILED",result.status());
        assertEquals("OVERDUE",installment.getStatus());
        verify(ledger).repay(any(),eq(loanId),eq(customerId),eq(new BigDecimal("8.00")),eq(new BigDecimal("1.00")),eq("USD"));
    }
}
