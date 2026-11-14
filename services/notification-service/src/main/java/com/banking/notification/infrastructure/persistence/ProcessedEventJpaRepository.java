package com.banking.notification.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, UUID> {
    @Modifying
    @Query(value = "INSERT INTO processed_events(event_id, event_type, processed_at) VALUES (:eventId, :eventType, :processedAt) ON CONFLICT (event_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId, @Param("eventType") String eventType, @Param("processedAt") java.time.Instant processedAt);
}
