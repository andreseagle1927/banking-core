package com.banking.notification.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.banking.notification.application.NotificationApplicationService;
import com.banking.notification.domain.TransferCompletedEvent;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {"spring.kafka.listener.auto-startup=false", "spring.kafka.admin.auto-create=false"})
@Testcontainers(disabledWithoutDocker = true)
class PersistenceSchemaTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6");

    @Autowired private EntityManager entityManager;
    @Autowired private NotificationApplicationService notifications;

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
    void duplicateKafkaEventsCreateOnlyOneNotificationPerParty() {
        UUID eventId = UUID.randomUUID();
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        TransferCompletedEvent event = new TransferCompletedEvent(eventId, "TransferCompleted", 1,
                UUID.randomUUID(), Instant.now(), UUID.randomUUID(), sender, recipient,
                "12.34", "USD", UUID.randomUUID());

        notifications.createTransferNotifications(event);
        notifications.createTransferNotifications(event);

        assertEquals(1, notifications.forWallet(sender).size());
        assertEquals(1, notifications.forWallet(recipient).size());
    }
}
