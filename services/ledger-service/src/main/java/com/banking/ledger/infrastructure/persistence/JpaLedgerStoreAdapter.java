package com.banking.ledger.infrastructure.persistence;

import com.banking.ledger.application.port.out.LedgerStore;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLedgerStoreAdapter implements LedgerStore {
    private static final UUID PLATFORM_FUNDING_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final LedgerAccountJpaRepository accounts;
    private final AccountBalanceJpaRepository balances;
    private final JournalTransactionJpaRepository journals;
    private final JournalEntryJpaRepository entries;

    public JpaLedgerStoreAdapter(LedgerAccountJpaRepository accounts, AccountBalanceJpaRepository balances,
                                 JournalTransactionJpaRepository journals, JournalEntryJpaRepository entries) {
        this.accounts = accounts; this.balances = balances; this.journals = journals; this.entries = entries;
    }

    @Override
    public UUID createWalletAccount(UUID walletId, String currency) {
        UUID proposedId = UUID.randomUUID();
        accounts.insertAccountIfAbsent(proposedId, walletId, currency, "CUSTOMER_WALLET");
        UUID accountId = accounts.findByOwnerIdAndCurrency(walletId, currency)
                .orElseThrow(() -> new IllegalStateException("Unable to provision wallet ledger account")).getId();
        balances.insertIfAbsent(accountId);
        return accountId;
    }

    @Override
    public UUID ensurePlatformFundingAccount(String currency) {
        return ensurePlatformAccount(currency,"PLATFORM_FUNDING");
    }

    @Override
    public UUID ensurePlatformAccount(String currency,String accountType) {
        UUID ownerId = UUID.nameUUIDFromBytes(("core-banking-platform:" + accountType).getBytes(StandardCharsets.UTF_8));
        UUID id = UUID.nameUUIDFromBytes((ownerId + ":" + currency).getBytes(StandardCharsets.UTF_8));
        accounts.insertAccountIfAbsent(id, ownerId, currency, accountType);
        UUID accountId = accounts.findByOwnerIdAndCurrency(ownerId, currency)
                .orElseThrow(() -> new IllegalStateException("Unable to provision platform funding account")).getId();
        balances.insertIfAbsent(accountId);
        return accountId;
    }

    @Override
    public Optional<UUID> findActiveWalletAccount(UUID walletId, String currency) {
        return accounts.findByOwnerIdAndCurrencyAndStatusAndAccountType(walletId, currency, "ACTIVE", "CUSTOMER_WALLET")
                .map(LedgerAccountEntity::getId);
    }

    @Override public Optional<Long> findWalletBalance(UUID walletId, String currency) {
        return accounts.findActiveWalletBalance(walletId, currency);
    }

    @Override public void setWalletAccountStatus(UUID walletId, String currency, String status) {
        LedgerAccountEntity account=accounts.findByOwnerIdAndCurrencyAndAccountType(walletId,currency,"CUSTOMER_WALLET")
                .orElseThrow(()->new LedgerStore.AccountStateException("Wallet ledger account does not exist"));
        AccountBalanceEntity balance=balances.findLockedByAccountId(account.getId())
                .orElseThrow(()->new LedgerStore.AccountStateException("Wallet ledger balance row is missing"));
        if ("CLOSED".equals(status) && balance.getBalanceMinor()!=0L)
            throw new LedgerStore.AccountStateException("Wallet currency must have a zero balance before closure");
        account.setStatus(status);
    }

    @Override
    public Optional<JournalSnapshot> findJournalByOperationId(UUID operationId) {
        return journals.findByOperationId(operationId)
                .map(journal -> new JournalSnapshot(journal.getId(), journal.getRequestHash(), journal.getTransactionType()));
    }

    @Override
    public Optional<Long> lockBalance(UUID accountId) {
        return balances.findLockedByAccountId(accountId).map(AccountBalanceEntity::getBalanceMinor);
    }

    @Override
    public void insertJournal(UUID journalId, UUID operationId, String type, String requestHash) {
        journals.save(new JournalTransactionEntity(journalId, operationId, type, requestHash, Instant.now()));
    }

    @Override
    public void appendEntry(UUID journalId, UUID accountId, long debitMinor, long creditMinor) {
        entries.save(new JournalEntryEntity(UUID.randomUUID(), journalId, accountId, debitMinor, creditMinor));
    }

    @Override
    public void adjustBalance(UUID accountId, long deltaMinor) {
        AccountBalanceEntity balance = balances.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Ledger balance row is missing"));
        balance.adjust(deltaMinor);
    }
}
