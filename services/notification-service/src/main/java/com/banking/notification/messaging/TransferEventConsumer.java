package com.banking.notification.messaging;

import com.banking.notification.application.NotificationApplicationService;
import com.banking.notification.domain.TransferCompletedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransferEventConsumer {
    private final NotificationApplicationService notifications;
    private final ObjectMapper objectMapper;
    public TransferEventConsumer(NotificationApplicationService notifications, ObjectMapper objectMapper) {
        this.notifications = notifications;
        this.objectMapper = objectMapper;
    }
    @KafkaListener(topics = "transfers.events.v1", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        try { notifications.createTransferNotifications(objectMapper.readValue(payload, TransferCompletedEvent.class)); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Invalid TransferCompleted event JSON", ex); }
    }
}
