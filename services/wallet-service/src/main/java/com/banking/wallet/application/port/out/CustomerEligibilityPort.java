package com.banking.wallet.application.port.out;
import java.util.UUID;
public interface CustomerEligibilityPort { boolean isActive(UUID customerId); }
