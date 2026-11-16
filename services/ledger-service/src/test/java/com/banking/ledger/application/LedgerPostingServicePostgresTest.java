package com.banking.ledger.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class LedgerPostingServicePostgresTest {
    private static final UUID ALICE = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-4222-8222-222222222222");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private LedgerPostingService ledger;
    @Autowired private PlatformTransactionManager transactionManager;
    private TransactionTemplate transactions;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void clearDatabase() {
        transactions = new TransactionTemplate(transactionManager);
        jdbc.execute("TRUNCATE TABLE journal_entries, journal_transactions, account_balances, ledger_accounts CASCADE");
        transactions.executeWithoutResult(status -> {
            ledger.createAccount(ALICE, "USD");
            ledger.createAccount(BOB, "USD");
            ledger.fund(UUID.randomUUID(), ALICE, new BigDecimal("500.00"), "USD");
        });
    }

    @Test
    void postsBalancedTransferAndConservesBalances() {
        UUID operationId = UUID.randomUUID();
        Map<String, Object> result = transactions.execute(status ->
                ledger.transfer(operationId, ALICE, BOB, new BigDecimal("100.00"), "USD"));

        assertEquals(40000L, balance(ALICE));
        assertEquals(10000L, balance(BOB));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT COALESCE(SUM(debit_minor-credit_minor), 0) FROM journal_entries", Long.class));
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM journal_entries WHERE journal_transaction_id=?",
                Integer.class, result.get("ledgerTransactionId")));
    }

    @Test
    void simultaneousDebitsCannotSpendTheSameFundsTwice() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first = transferAttempt(UUID.randomUUID());
            Callable<Boolean> second = transferAttempt(UUID.randomUUID());
            Future<Boolean> firstResult = workers.submit(first);
            Future<Boolean> secondResult = workers.submit(second);

            int completed = (firstResult.get() ? 1 : 0) + (secondResult.get() ? 1 : 0);
            assertEquals(1, completed);
            assertEquals(10000L, balance(ALICE));
            assertEquals(40000L, balance(BOB));
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM journal_transactions WHERE transaction_type='TRANSFER'", Integer.class));
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void retriesWithSameOperationIdReturnTheOriginalPosting() {
        UUID operationId = UUID.randomUUID();
        Map<String, Object> first = transactions.execute(status ->
                ledger.transfer(operationId, ALICE, BOB, new BigDecimal("100.00"), "USD"));
        Map<String, Object> retry = transactions.execute(status ->
                ledger.transfer(operationId, ALICE, BOB, new BigDecimal("100.00"), "USD"));

        assertEquals(first.get("ledgerTransactionId"), retry.get("ledgerTransactionId"));
        assertEquals(40000L, balance(ALICE));
        assertEquals(10000L, balance(BOB));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM journal_transactions WHERE operation_id=?", Integer.class, operationId));
        assertThrows(LedgerPostingService.IdempotencyConflict.class, () -> transactions.execute(status ->
                ledger.transfer(operationId, ALICE, BOB, new BigDecimal("101.00"), "USD")));
    }

    @Test
    void insufficientFundsRollBacksTheJournalAndCurrencyPrecisionIsEnforced() {
        UUID operationId = UUID.randomUUID();
        assertThrows(LedgerPostingService.InsufficientFunds.class, () -> transactions.execute(status ->
                ledger.transfer(operationId, ALICE, BOB, new BigDecimal("600.00"), "USD")));

        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM journal_transactions WHERE operation_id=?", Integer.class, operationId));
        assertEquals(50000L, balance(ALICE));
        assertThrows(LedgerPostingService.InvalidAccount.class, () -> ledger.createAccount(UUID.randomUUID(), "JPY"));
        assertThrows(LedgerPostingService.InvalidAmount.class, () -> transactions.execute(status ->
                ledger.fund(UUID.randomUUID(), ALICE, new BigDecimal("1.001"), "USD")));
    }

    @Test
    void journalEntriesCannotBeUpdatedOrDeleted() {
        transactions.execute(status -> ledger.transfer(UUID.randomUUID(), ALICE, BOB,
                new BigDecimal("1.00"), "USD"));
        UUID entryId = jdbc.queryForObject("SELECT id FROM journal_entries LIMIT 1", UUID.class);

        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE journal_entries SET debit_minor=debit_minor+1 WHERE id=?", entryId));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "DELETE FROM journal_entries WHERE id=?", entryId));
    }

    @Test
    void loanDisbursementAndRepaymentAreBalancedAndIdempotent() {
        UUID loanId=UUID.randomUUID(),disbursementId=UUID.randomUUID();
        Map<String,Object> first=transactions.execute(status->ledger.disburseLoan(disbursementId,loanId,ALICE,new BigDecimal("100.00"),"USD"));
        Map<String,Object> retry=transactions.execute(status->ledger.disburseLoan(disbursementId,loanId,ALICE,new BigDecimal("100.00"),"USD"));
        assertEquals(first.get("ledgerTransactionId"),retry.get("ledgerTransactionId"));
        assertEquals(60000L,balance(ALICE));

        UUID repaymentId=UUID.randomUUID();
        transactions.execute(status->ledger.repayLoan(repaymentId,loanId,ALICE,new BigDecimal("20.00"),new BigDecimal("2.34"),"USD"));
        assertEquals(57766L,balance(ALICE));
        assertEquals(8000L,jdbc.queryForObject("SELECT b.balance_minor FROM account_balances b JOIN ledger_accounts a ON a.id=b.ledger_account_id WHERE a.account_type='LOAN_RECEIVABLE' AND a.currency='USD'",Long.class));
        assertEquals(0L,jdbc.queryForObject("SELECT COALESCE(SUM(debit_minor-credit_minor),0) FROM journal_entries",Long.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM journal_transactions WHERE operation_id=?",Integer.class,disbursementId));
    }

    @Test
    void insufficientLoanRepaymentRollsBackEveryJournalEntry() {
        UUID loanId=UUID.randomUUID(),operationId=UUID.randomUUID();
        assertThrows(LedgerPostingService.InsufficientFunds.class,()->transactions.execute(status->
                ledger.repayLoan(operationId,loanId,ALICE,new BigDecimal("600.00"),new BigDecimal("10.00"),"USD")));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM journal_transactions WHERE operation_id=?",Integer.class,operationId));
        assertEquals(50000L,balance(ALICE));
    }

    @Test
    void blockedWalletCannotBeDebitedAndNonzeroAccountCannotClose() {
        transactions.executeWithoutResult(status->ledger.setWalletAccountStatus(ALICE,"USD","BLOCKED"));
        assertThrows(LedgerPostingService.InvalidAccount.class,()->transactions.execute(status->
                ledger.transfer(UUID.randomUUID(),ALICE,BOB,new BigDecimal("1.00"),"USD")));
        assertThrows(LedgerPostingService.InvalidAccount.class,()->transactions.executeWithoutResult(status->
                ledger.setWalletAccountStatus(ALICE,"USD","CLOSED")));
    }

    private Callable<Boolean> transferAttempt(UUID operationId) {
        return () -> {
            try {
                transactions.execute(status -> ledger.transfer(operationId, ALICE, BOB,
                        new BigDecimal("400.00"), "USD"));
                return true;
            } catch (LedgerPostingService.InsufficientFunds expected) {
                return false;
            }
        };
    }

    private long balance(UUID walletId) {
        return jdbc.queryForObject("SELECT b.balance_minor FROM account_balances b JOIN ledger_accounts a "
                + "ON a.id=b.ledger_account_id WHERE a.owner_id=? AND a.currency='USD'", Long.class, walletId);
    }
}
