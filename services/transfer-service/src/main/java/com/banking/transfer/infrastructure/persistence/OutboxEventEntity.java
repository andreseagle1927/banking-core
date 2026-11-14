package com.banking.transfer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id @Column(name = "event_id") private UUID eventId;
    @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
    @Column(name = "event_type", nullable = false, length = 128) private String eventType;
    @Column(name = "schema_version", nullable = false) private int schemaVersion;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private JsonNode payload;
    @Column(name = "published_at") private Instant publishedAt;

    protected OutboxEventEntity() { }
    public OutboxEventEntity(UUID eventId, UUID aggregateId, String eventType, int schemaVersion, Instant occurredAt, JsonNode payload) {
        this.eventId = eventId; this.aggregateId = aggregateId; this.eventType = eventType; this.schemaVersion = schemaVersion;
        this.occurredAt = occurredAt; this.payload = payload;
    }
    public UUID getEventId() { return eventId; }
    public JsonNode getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public void markPublished(Instant at) { publishedAt = at; }
}
