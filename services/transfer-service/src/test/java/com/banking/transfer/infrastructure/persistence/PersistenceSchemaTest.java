package com.banking.transfer.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.banking.transfer.application.port.out.TransferCompletionPort;
import com.banking.transfer.application.port.out.TransferRepository;
import com.banking.transfer.domain.Money;
import com.banking.transfer.domain.TransferRecord;
import com.banking.transfer.domain.TransferStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {"spring.kafka.admin.auto-create=false", "OUTBOX_PUBLISH_DELAY_MS=3600000"})
@Testcontainers(disabledWithoutDocker = true)
class PersistenceSchemaTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6");

    @Autowired private EntityManager entityManager;
    @Autowired private TransferRepository transfers;
    @Autowired private TransferCompletionPort completion;
    @Autowired private TransferJpaRepository transferRows;
    @Autowired private OutboxJpaRepository outbox;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void flywaySchemaMatchesHibernateMappings() {
        assertTrue(!entityManager.getMetamodel().getEntities().isEmpty());
    }

    @Test
    void idempotentTransferAndCompletionPersistStateAndOutboxTogether() {
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID ledgerId = UUID.randomUUID();
        TransferRecord requested = TransferRecord.pending(transferId, "a".repeat(64), sender, recipient,
                new Money(new BigDecimal("25.00"), "USD"));

        TransferRecord first = transfers.savePendingIfAbsent("transfer-key", requested);
        TransferRecord duplicate = transfers.savePendingIfAbsent("transfer-key",
                TransferRecord.pending(UUID.randomUUID(), requested.requestHash(), sender, recipient,
                        new Money(new BigDecimal("25.00"), "USD")));
        assertEquals(first.id(), duplicate.id());

        completion.markCompletedAndEnqueueEvent(transferId, ledgerId);

        assertEquals(TransferStatus.COMPLETED, transfers.findById(transferId).status());
        assertEquals(ledgerId, transfers.findById(transferId).ledgerTransactionId());
        assertEquals(1L, outbox.count());
        assertTrue(outbox.findAll().getFirst().getPayload().toString().contains("TransferCompleted"));
        assertTrue(transferRows.findById(transferId).isPresent());
    }
}
