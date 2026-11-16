package com.banking.notification.application.port.out;
import java.util.Optional;
import java.util.UUID;
public interface CustomerContactPort { Optional<String> emailFor(UUID walletId); }
