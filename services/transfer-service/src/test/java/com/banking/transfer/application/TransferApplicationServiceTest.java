package com.banking.transfer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.transfer.application.exception.TransferConflictException;
import com.banking.transfer.application.exception.LedgerUnavailableException;
import com.banking.transfer.application.port.out.LedgerPostingPort;
import com.banking.transfer.application.port.out.TransferCompletionPort;
import com.banking.transfer.application.port.out.TransferRepository;
import com.banking.transfer.domain.Money;
import com.banking.transfer.domain.TransferRecord;
import com.banking.transfer.domain.TransferStatus;
import java.math.BigDecimal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferApplicationServiceTest {
    @Mock TransferRepository transfers;
    @Mock LedgerPostingPort ledger;
    @Mock TransferCompletionPort completion;

    private TransferApplicationService service;
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID transferId = UUID.randomUUID();
    private final UUID ledgerId = UUID.randomUUID();

    @BeforeEach
    void setUp() { service = new TransferApplicationService(transfers, ledger, completion); }

    @Test
    void postsPendingTransferAndRecordsCompletion() {
        AtomicReference<TransferRecord> pendingRef = new AtomicReference<>();
        when(transfers.savePendingIfAbsent(eq("key-1"), any())).thenAnswer(invocation -> {
            TransferRecord pending = invocation.getArgument(1);
            pendingRef.set(pending);
            return pending;
        });
        when(ledger.postTransfer(any(), eq(alice), eq(bob), eq(new BigDecimal("25.00")), eq("USD"))).thenReturn(ledgerId);
        when(transfers.findById(any())).thenAnswer(invocation -> {
            TransferRecord pending = pendingRef.get();
            return new TransferRecord(pending.id(), pending.requestHash(), pending.senderWalletId(),
                    pending.recipientWalletId(), pending.amount(), pending.currency(), TransferStatus.COMPLETED,
                    ledgerId, null);
        });

        TransferRecord result = service.create("key-1", alice,
                new TransferCommand(bob, new BigDecimal("25.00"), "USD"));

        assertEquals(TransferStatus.COMPLETED, result.status());
        verify(completion).markCompletedAndEnqueueEvent(any(), eq(ledgerId));
    }

    @Test
    void rejectsReusedIdempotencyKeyWithDifferentRequest() {
        TransferRecord existing = record("different", TransferStatus.PENDING, null);
        when(transfers.savePendingIfAbsent(eq("key-1"), any())).thenReturn(existing);

        assertThrows(TransferConflictException.class, () -> service.create("key-1", alice,
                new TransferCommand(bob, new BigDecimal("30.00"), "USD")));
        verify(ledger, never()).postTransfer(any(), any(), any(), any(), any());
    }

    @Test
    void returnsExistingCompletedTransferWithoutPostingAgain() {
        TransferRecord existing = record(hash(alice + "|" + bob + "|25.00|USD"), TransferStatus.COMPLETED, ledgerId);
        when(transfers.savePendingIfAbsent(eq("key-1"), any())).thenReturn(existing);

        assertEquals(existing, service.create("key-1", alice,
                new TransferCommand(bob, new BigDecimal("25.00"), "USD")));
        verify(ledger, never()).postTransfer(any(), any(), any(), any(), any());
        verify(completion, never()).markCompletedAndEnqueueEvent(any(), any());
    }

    @Test
    void retriesPendingTransferWithTheSameLedgerOperationAfterLostResponse() {
        String requestHash = hash(alice + "|" + bob + "|25.00|USD");
        TransferRecord pending = record(requestHash, TransferStatus.PENDING, null);
        TransferRecord completed = record(requestHash, TransferStatus.COMPLETED, ledgerId);
        when(transfers.savePendingIfAbsent(eq("key-1"), any())).thenReturn(pending, pending);
        when(ledger.postTransfer(eq(transferId), eq(alice), eq(bob), eq(new BigDecimal("25.00")), eq("USD")))
                .thenThrow(new LedgerUnavailableException(new IOException("response lost")))
                .thenReturn(ledgerId);
        when(transfers.findById(transferId)).thenReturn(pending, completed);

        assertEquals(TransferStatus.PENDING, service.create("key-1", alice,
                new TransferCommand(bob, new BigDecimal("25.00"), "USD")).status());
        assertEquals(TransferStatus.COMPLETED, service.create("key-1", alice,
                new TransferCommand(bob, new BigDecimal("25.00"), "USD")).status());

        verify(ledger, org.mockito.Mockito.times(2)).postTransfer(transferId, alice, bob,
                new BigDecimal("25.00"), "USD");
        verify(completion).markCompletedAndEnqueueEvent(transferId, ledgerId);
    }

    @Test
    void rejectsSelfTransfersBeforePersistence() {
        assertThrows(RuntimeException.class, () -> service.create("key-1", alice,
                new TransferCommand(alice, new BigDecimal("25.00"), "USD")));
        verify(transfers, never()).savePendingIfAbsent(any(), any());
    }

    private TransferRecord record(String requestHash, TransferStatus status, UUID postingId) {
        return new TransferRecord(transferId, requestHash, alice, bob, new BigDecimal("25.00"), "USD", status,
                postingId, null);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
