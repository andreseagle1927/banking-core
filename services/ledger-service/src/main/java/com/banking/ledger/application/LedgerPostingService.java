package com.banking.ledger.application;

import com.banking.ledger.application.port.out.LedgerStore;
import com.banking.ledger.application.port.out.LedgerStore.JournalSnapshot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Currency;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerPostingService {
    private final LedgerStore store;
    public LedgerPostingService(LedgerStore store) { this.store = store; }

    @Transactional
    public void createAccount(UUID walletId, String currency) {
        ensureCurrency(currency);
        store.createWalletAccount(walletId, currency);
    }

    @Transactional
    public Map<String, Object> fund(UUID operationId, UUID walletId, BigDecimal amount, String currency) {
        long minor = toMinor(amount, currency);
        String requestHash = hash("FUNDING", walletId.toString(), Long.toString(minor), currency);
        Map<String, Object> existing = existing(operationId, requestHash);
        if (existing != null) return existing;
        UUID customerAccount = accountId(walletId, currency);
        UUID fundingAccount = store.ensurePlatformFundingAccount(currency);
        UUID journalId = UUID.randomUUID();
        store.insertJournal(journalId, operationId, "SIMULATED_FUNDING", requestHash);
        lockAccounts(List.of(customerAccount, fundingAccount));
        store.appendEntry(journalId, fundingAccount, minor, 0);
        store.appendEntry(journalId, customerAccount, 0, minor);
        store.adjustBalance(customerAccount, minor);
        store.adjustBalance(fundingAccount, -minor);
        return result(journalId, minor, currency);
    }

    @Transactional
    public Map<String, Object> transfer(UUID operationId, UUID sender, UUID recipient, BigDecimal amount, String currency) {
        if (sender.equals(recipient)) throw new InvalidAccount("Sender and recipient must differ");
        long minor = toMinor(amount, currency);
        String requestHash = hash("TRANSFER", sender.toString(), recipient.toString(), Long.toString(minor), currency);
        Map<String, Object> existing = existing(operationId, requestHash);
        if (existing != null) return existing;
        UUID debitAccount = accountId(sender, currency);
        UUID creditAccount = accountId(recipient, currency);
        UUID journalId = UUID.randomUUID();
        store.insertJournal(journalId, operationId, "TRANSFER", requestHash);
        Map<UUID, Long> balances = lockAccounts(List.of(debitAccount, creditAccount));
        long available = balances.get(debitAccount);
        if (available < minor) throw new InsufficientFunds("Insufficient spendable balance");
        store.appendEntry(journalId, debitAccount, minor, 0);
        store.appendEntry(journalId, creditAccount, 0, minor);
        store.adjustBalance(debitAccount, -minor);
        store.adjustBalance(creditAccount, minor);
        return result(journalId, minor, currency);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> balance(UUID walletId, String currency) {
        ensureCurrency(currency);
        long minor = store.findWalletBalance(walletId, currency)
                .orElseThrow(() -> new InvalidAccount("Account not found or inactive"));
        return Map.of("walletId", walletId, "currency", currency, "balanceMinor", minor);
    }

    private UUID accountId(UUID walletId, String currency) {
        return store.findActiveWalletAccount(walletId, currency)
                .orElseThrow(() -> new InvalidAccount("Active wallet account not found for " + currency));
    }

    private Map<UUID, Long> lockAccounts(List<UUID> accountIds) {
        Map<UUID, Long> balances = new HashMap<>();
        accountIds.stream().distinct().sorted().forEach(id -> balances.put(id,
                store.lockBalance(id).orElseThrow(() -> new InvalidAccount("Account balance missing"))));
        return balances;
    }

    private Map<String, Object> existing(UUID operationId, String requestHash) {
        Optional<JournalSnapshot> previous = store.findJournalByOperationId(operationId);
        if (previous.isEmpty()) return null;
        JournalSnapshot journal = previous.get();
        if (!requestHash.equals(journal.requestHash())) throw new IdempotencyConflict("Operation ID was already used for a different request");
        return Map.of("ledgerTransactionId", journal.id(), "status", "POSTED", "operationId", operationId, "type", journal.transactionType());
    }

    private Map<String, Object> result(UUID journalId, long minor, String currency) {
        return Map.of("ledgerTransactionId", journalId, "status", "POSTED", "amountMinor", minor, "currency", currency);
    }

    private long toMinor(BigDecimal amount, String currency) {
        ensureCurrency(currency);
        try { return amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact(); }
        catch (ArithmeticException ex) { throw new InvalidAmount("Amount must have at most two decimal places and fit in Ledger minor units"); }
    }

    private void ensureCurrency(String currency) {
        if (currency == null || !currency.matches("[A-Z]{3}")) throw new InvalidAccount("Currency must be an uppercase ISO-style code");
        Currency isoCurrency;
        try { isoCurrency = Currency.getInstance(currency); }
        catch (IllegalArgumentException ex) { throw new InvalidAccount("Unsupported currency: " + currency); }
        if (isoCurrency.getDefaultFractionDigits() != 2) throw new InvalidAccount("V1 supports currencies with two minor-unit digits only");
    }

    private String hash(String... values) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(String.join("|", values).getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    public static class InsufficientFunds extends RuntimeException { public InsufficientFunds(String message) { super(message); } }
    public static class InvalidAccount extends RuntimeException { public InvalidAccount(String message) { super(message); } }
    public static class IdempotencyConflict extends RuntimeException { public IdempotencyConflict(String message) { super(message); } }
    public static class InvalidAmount extends RuntimeException { public InvalidAmount(String message) { super(message); } }
}
