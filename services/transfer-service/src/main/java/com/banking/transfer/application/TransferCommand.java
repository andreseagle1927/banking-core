package com.banking.transfer.application;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCommand(UUID recipientWalletId, BigDecimal amount, String currency) { }
