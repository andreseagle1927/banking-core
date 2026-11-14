package com.banking.transfer.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerPostingPort {
    UUID postTransfer(UUID operationId, UUID senderWalletId, UUID recipientWalletId,
                      BigDecimal amount, String currency);
}
