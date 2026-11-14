package com.banking.ledger.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntryEntity {
    @Id private UUID id;
    @Column(name = "journal_transaction_id", nullable = false) private UUID journalTransactionId;
    @Column(name = "ledger_account_id", nullable = false) private UUID ledgerAccountId;
    @Column(name = "debit_minor", nullable = false) private long debitMinor;
    @Column(name = "credit_minor", nullable = false) private long creditMinor;

    protected JournalEntryEntity() { }
    public JournalEntryEntity(UUID id, UUID transactionId, UUID accountId, long debitMinor, long creditMinor) {
        this.id = id; this.journalTransactionId = transactionId; this.ledgerAccountId = accountId; this.debitMinor = debitMinor; this.creditMinor = creditMinor;
    }
}
