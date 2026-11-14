package com.banking.ledger.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "account_balances")
public class AccountBalanceEntity {
    @Id @Column(name = "ledger_account_id") private UUID ledgerAccountId;
    @Column(name = "balance_minor", nullable = false) private long balanceMinor;
    @Version @Column(nullable = false) private long version;

    protected AccountBalanceEntity() { }
    public AccountBalanceEntity(UUID ledgerAccountId, long balanceMinor) { this.ledgerAccountId = ledgerAccountId; this.balanceMinor = balanceMinor; }
    public UUID getLedgerAccountId() { return ledgerAccountId; }
    public long getBalanceMinor() { return balanceMinor; }
    public void adjust(long deltaMinor) { balanceMinor = Math.addExact(balanceMinor, deltaMinor); }
}
