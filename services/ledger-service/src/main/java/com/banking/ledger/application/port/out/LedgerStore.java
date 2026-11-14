package com.banking.ledger.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerStore {
    UUID createWalletAccount(UUID walletId, String currency);
    UUID ensurePlatformFundingAccount(String currency);
    Optional<UUID> findActiveWalletAccount(UUID walletId, String currency);
    Optional<Long> findWalletBalance(UUID walletId, String currency);
    Optional<JournalSnapshot> findJournalByOperationId(UUID operationId);
    Optional<Long> lockBalance(UUID accountId);
    void insertJournal(UUID journalId, UUID operationId, String type, String requestHash);
    void appendEntry(UUID journalId, UUID accountId, long debitMinor, long creditMinor);
    void adjustBalance(UUID accountId, long deltaMinor);

    record JournalSnapshot(UUID id, String requestHash, String transactionType) { }
}
