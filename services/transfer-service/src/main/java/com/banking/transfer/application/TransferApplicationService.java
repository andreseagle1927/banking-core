package com.banking.transfer.application;

import com.banking.transfer.application.exception.LedgerCallException;
import com.banking.transfer.application.exception.LedgerUnavailableException;
import com.banking.transfer.application.exception.TransferConflictException;
import com.banking.transfer.application.exception.TransferNotFoundException;
import com.banking.transfer.application.port.out.LedgerPostingPort;
import com.banking.transfer.application.port.out.TransferCompletionPort;
import com.banking.transfer.application.port.out.TransferRepository;
import com.banking.transfer.domain.TransferRecord;
import com.banking.transfer.domain.Money;
import com.banking.transfer.domain.TransferRuleViolation;
import com.banking.transfer.domain.TransferStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransferApplicationService {
    private final TransferRepository transfers;
    private final LedgerPostingPort ledger;
    private final TransferCompletionPort completion;

    public TransferApplicationService(TransferRepository transfers, LedgerPostingPort ledger, TransferCompletionPort completion) {
        this.transfers = transfers;
        this.ledger = ledger;
        this.completion = completion;
    }

    public TransferRecord create(String idempotencyKey, UUID senderWalletId, TransferCommand command) {
        Money money = validate(idempotencyKey, senderWalletId, command);
        String requestHash = digest(senderWalletId + "|" + command.recipientWalletId() + "|" + money.amount().toPlainString() + "|" + money.currency());
        TransferRecord pending = TransferRecord.pending(UUID.randomUUID(), requestHash, senderWalletId,
                command.recipientWalletId(), money);
        TransferRecord transfer = transfers.savePendingIfAbsent(idempotencyKey, pending);
        if (!transfer.requestHash().equals(requestHash))
            throw new TransferConflictException("Idempotency-Key was already used for a different request");
        if (transfer.status() != TransferStatus.PENDING) return transfer;

        try {
            UUID ledgerId = ledger.postTransfer(transfer.id(), senderWalletId, command.recipientWalletId(),
                    transfer.amount(), transfer.currency());
            completion.markCompletedAndEnqueueEvent(transfer.id(), ledgerId);
        } catch (LedgerCallException ex) {
            if (ex.statusCode() == 422) transfers.markRejected(transfer.id(), ex.getMessage());
            else if (ex.statusCode() == 409) throw new TransferConflictException("Ledger operation ID conflict");
            else return transfers.findById(transfer.id());
        } catch (LedgerUnavailableException ex) {
            return transfers.findById(transfer.id());
        }
        return transfers.findById(transfer.id());
    }

    public TransferRecord get(UUID transferId, UUID callerWalletId) {
        return transfers.findVisibleTo(transferId, callerWalletId).orElseThrow(TransferNotFoundException::new);
    }

    private Money validate(String idempotencyKey, UUID sender, TransferCommand command) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128)
            throw new TransferRuleViolation("Idempotency key must be between 1 and 128 characters");
        if (sender == null || command == null || command.recipientWalletId() == null)
            throw new TransferRuleViolation("Sender and recipient are required");
        if (sender.equals(command.recipientWalletId())) throw new TransferRuleViolation("Sender and recipient must differ");
        return new Money(command.amount(), command.currency());
    }

    private String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
