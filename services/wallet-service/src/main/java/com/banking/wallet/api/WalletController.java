package com.banking.wallet.api;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1/wallets") public class WalletController {
    @GetMapping("/me") public Map<String,String> me() { return Map.of("status", "skeleton", "service", "wallet-service", "note", "Wallet lifecycle is scaffolded; authoritative balances belong to Ledger."); }
}
