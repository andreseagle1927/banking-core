package com.banking.loan.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LoanRepaymentScheduler {
    private static final Logger log=LoggerFactory.getLogger(LoanRepaymentScheduler.class);
    private final LoanWorkflowService loans;
    public LoanRepaymentScheduler(LoanWorkflowService loans){this.loans=loans;}
    @Scheduled(cron="${loan.repayments.cron:0 0 9 * * *}",zone="UTC")
    public void processDueInstallments(){
        try { int processed=loans.processDueInstallments(); if(processed>0)log.info("Processed {} due loan installments",processed); }
        catch(RuntimeException ex){log.error("Due installment processing failed; next scheduled run will retry safely",ex);}
    }
}
