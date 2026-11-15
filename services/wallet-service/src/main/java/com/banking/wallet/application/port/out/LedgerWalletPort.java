package com.banking.wallet.application.port.out;

import java.util.Map;
import java.util.UUID;

public interface LedgerWalletPort {
    void createAccount(UUID walletId, String currency);
    void setAccountStatus(UUID walletId, String currency, String status);
    long balance(UUID walletId, String currency);
}
