package com.banking.notification.infrastructure.persistence;

import com.banking.notification.application.port.out.NotificationDeliveryPort;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaNotificationDeliveryQueue implements NotificationDeliveryPort {
    private final NotificationDeliveryJpaRepository deliveries;
    public JpaNotificationDeliveryQueue(NotificationDeliveryJpaRepository deliveries){this.deliveries=deliveries;}
    @Override public void enqueue(UUID notificationId,String email,String subject,String message){
        deliveries.save(new NotificationDeliveryEntity(UUID.randomUUID(),notificationId,email,subject,message,Instant.now()));
    }
}
