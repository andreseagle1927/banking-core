package com.banking.wallet.application;

import com.banking.wallet.application.port.out.LedgerWalletPort;
import com.banking.wallet.application.port.out.CustomerEligibilityPort;
import com.banking.wallet.infrastructure.persistence.WalletCurrencyEntity;
import com.banking.wallet.infrastructure.persistence.WalletCurrencyJpaRepository;
import com.banking.wallet.infrastructure.persistence.WalletEntity;
import com.banking.wallet.infrastructure.persistence.WalletJpaRepository;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WalletApplicationService {
    private final WalletJpaRepository wallets;
    private final WalletCurrencyJpaRepository currencies;
    private final LedgerWalletPort ledger;
    private final CustomerEligibilityPort customers;
    public WalletApplicationService(WalletJpaRepository wallets, WalletCurrencyJpaRepository currencies, LedgerWalletPort ledger,CustomerEligibilityPort customers) {
        this.wallets=wallets; this.currencies=currencies; this.ledger=ledger;this.customers=customers;
    }

    @Transactional
    public WalletEntity open(UUID customerId) {
        if(!customers.isActive(customerId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"An active customer profile is required to open a wallet");
        WalletEntity wallet=wallets.findByCustomerId(customerId).orElseGet(()->{
            try { return wallets.save(new WalletEntity(customerId,customerId,"ACTIVE",java.time.Instant.now())); }
            catch (DataIntegrityViolationException race) { return wallets.findByCustomerId(customerId).orElseThrow(()->race); }
        });
        if ("CLOSED".equals(wallet.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Closed wallets cannot be reopened");
        return wallet;
    }

    @Transactional(readOnly=true)
    public WalletEntity get(UUID customerId) { return wallets.findByCustomerId(customerId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Wallet not found; create it first")); }

    @Transactional
    public WalletCurrencyEntity enableCurrency(UUID customerId,String rawCurrency) {
        WalletEntity wallet=open(customerId);
        if (!"ACTIVE".equals(wallet.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Wallet is not active");
        String currency=validateCurrency(rawCurrency);
        ledger.createAccount(wallet.getId(),currency);
        WalletCurrencyEntity pocket=currencies.findByWalletIdAndCurrency(wallet.getId(),currency).orElse(null);
        if (pocket==null) {
            try { pocket=currencies.save(new WalletCurrencyEntity(UUID.randomUUID(),wallet.getId(),currency,"ACTIVE",java.time.Instant.now())); }
            catch (DataIntegrityViolationException race) { pocket=currencies.findByWalletIdAndCurrency(wallet.getId(),currency).orElseThrow(()->race); }
        } else if ("CLOSED".equals(pocket.getStatus())) {
            ledger.setAccountStatus(wallet.getId(),currency,"ACTIVE"); pocket.setStatus("ACTIVE");
        }
        return pocket;
    }

    @Transactional
    public WalletEntity changeStatus(UUID customerId,String target) {
        if (!List.of("ACTIVE","BLOCKED","CLOSED").contains(target)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Status must be ACTIVE, BLOCKED, or CLOSED");
        WalletEntity wallet=get(customerId);
        if ("CLOSED".equals(wallet.getStatus()) && !"CLOSED".equals(target)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Closed wallets cannot be reopened");
        if (wallet.getStatus().equals(target)) return wallet;
        List<WalletCurrencyEntity> pockets=currencies.findByWalletId(wallet.getId());
        for (WalletCurrencyEntity pocket:pockets) {
            if ("CLOSED".equals(pocket.getStatus()) && !"CLOSED".equals(target)) continue;
            String next="CLOSED".equals(target)?"CLOSED":("BLOCKED".equals(target)?"BLOCKED":"ACTIVE");
            try { ledger.setAccountStatus(wallet.getId(),pocket.getCurrency(),next); }
            catch (RuntimeException failure) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Ledger could not apply wallet status; retry the request",failure); }
            pocket.setStatus(next);
        }
        wallet.setStatus(target);
        return wallet;
    }

    @Transactional(readOnly=true)
    public List<CurrencyBalance> balances(UUID customerId) {
        WalletEntity wallet=get(customerId);
        return currencies.findByWalletIdOrderByCurrency(wallet.getId()).stream()
                .filter(p->!"CLOSED".equals(p.getStatus()))
                .map(p->new CurrencyBalance(p.getCurrency(),p.getStatus(),ledger.balance(wallet.getId(),p.getCurrency())))
                .toList();
    }

    @Transactional(readOnly=true)
    public List<WalletCurrencyEntity> listCurrencies(UUID customerId) {
        WalletEntity wallet=get(customerId); return currencies.findByWalletIdOrderByCurrency(wallet.getId());
    }

    private String validateCurrency(String input) {
        String value=input==null?"":input.toUpperCase(Locale.ROOT);
        try { if (!value.matches("[A-Z]{3}") || Currency.getInstance(value).getDefaultFractionDigits()!=2) throw new IllegalArgumentException(); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"V1 supports ISO currencies with two minor-unit digits only"); }
        return value;
    }
    public record CurrencyBalance(String currency,String status,long balanceMinor) { }
}
