package com.banking.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryJpaRepository extends JpaRepository<NotificationDeliveryEntity,UUID> {
    List<NotificationDeliveryEntity> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(String status,Instant now);
}
