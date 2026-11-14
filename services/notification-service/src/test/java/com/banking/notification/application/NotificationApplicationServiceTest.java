package com.banking.notification.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.notification.application.port.out.NotificationStore;
import com.banking.notification.domain.TransferCompletedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {
    @Mock NotificationStore store;
    private NotificationApplicationService service;
    private final UUID sender = UUID.randomUUID();
    private final UUID recipient = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() { service = new NotificationApplicationService(store); }

    @Test
    void createsOneNotificationForEachPartyForANewEvent() {
        when(store.markEventProcessed(eq(eventId), eq("TransferCompleted"), any())).thenReturn(true);

        service.createTransferNotifications(validEvent());

        verify(store).saveNotification(any(), eq(sender));
        verify(store).saveNotification(any(), eq(recipient));
    }

    @Test
    void duplicateEventDoesNotCreateDuplicateNotifications() {
        when(store.markEventProcessed(eq(eventId), eq("TransferCompleted"), any())).thenReturn(false);

        service.createTransferNotifications(validEvent());

        verify(store, never()).saveNotification(any(), any());
    }

    @Test
    void rejectsUnsupportedPrecisionAndMalformedEvents() {
        TransferCompletedEvent yen = new TransferCompletedEvent(eventId, "TransferCompleted", 1,
                UUID.randomUUID(), Instant.now(), UUID.randomUUID(), sender, recipient,
                "100", "JPY", UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> service.createTransferNotifications(yen));
        assertThrows(IllegalArgumentException.class, () -> service.createTransferNotifications(null));
    }

    private TransferCompletedEvent validEvent() {
        return new TransferCompletedEvent(eventId, "TransferCompleted", 1, UUID.randomUUID(),
                Instant.now(), UUID.randomUUID(), sender, recipient, "12.50", "USD", UUID.randomUUID());
    }
}
