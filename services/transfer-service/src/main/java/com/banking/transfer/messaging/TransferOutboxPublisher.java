package com.banking.transfer.messaging;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.banking.transfer.infrastructure.persistence.OutboxEventEntity;
import com.banking.transfer.infrastructure.persistence.OutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TransferOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(TransferOutboxPublisher.class);
    private static final String TOPIC = "transfers.events.v1";
    private final OutboxJpaRepository outbox;
    private final KafkaTemplate<Object, Object> kafka;
    public TransferOutboxPublisher(OutboxJpaRepository outbox, KafkaTemplate<Object, Object> kafka) { this.outbox = outbox; this.kafka = kafka; }

    @Scheduled(fixedDelayString = "${OUTBOX_PUBLISH_DELAY_MS:1000}")
    public void publishPending() {
        List<OutboxEventEntity> rows = outbox.findTop50ByPublishedAtIsNullOrderByOccurredAtAsc();
        for (OutboxEventEntity row : rows) {
            try {
                kafka.send(TOPIC, row.getEventId().toString(), row.getPayload().toString()).get(10, TimeUnit.SECONDS);
                row.markPublished(java.time.Instant.now());
                outbox.save(row);
            } catch (Exception ex) {
                log.warn("Transfer outbox event {} remains unpublished and will be retried", row.getEventId(), ex);
                return;
            }
        }
    }
}
