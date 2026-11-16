package com.banking.notification.application.port.out;
import java.util.UUID;
public interface NotificationDeliveryPort { void enqueue(UUID notificationId,String email,String subject,String message); }
