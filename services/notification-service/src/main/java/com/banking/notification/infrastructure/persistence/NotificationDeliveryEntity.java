package com.banking.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="notification_deliveries",uniqueConstraints=@UniqueConstraint(columnNames="notification_id"))
public class NotificationDeliveryEntity {
    @Id private UUID id;
    @Column(name="notification_id",nullable=false,unique=true) private UUID notificationId;
    @Column(name="recipient_email",nullable=false,length=254) private String recipientEmail;
    @Column(nullable=false,length=200) private String subject;
    @Column(nullable=false,length=1000) private String message;
    @Column(nullable=false,length=16) private String status;
    @Column(nullable=false) private int attempts;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="next_attempt_at",nullable=false) private Instant nextAttemptAt;
    @Column(name="attempted_at") private Instant attemptedAt;
    @Column(name="last_error",length=500) private String lastError;
    protected NotificationDeliveryEntity(){}
    public NotificationDeliveryEntity(UUID id,UUID notificationId,String recipientEmail,String subject,String message,Instant now){
        this.id=id;this.notificationId=notificationId;this.recipientEmail=recipientEmail;this.subject=subject;this.message=message;
        this.status="PENDING";this.attempts=0;this.createdAt=now;this.nextAttemptAt=now;
    }
    public UUID getId(){return id;} public String getRecipientEmail(){return recipientEmail;} public String getSubject(){return subject;} public String getMessage(){return message;} public int getAttempts(){return attempts;}
    public void sent(Instant at){this.status="SENT";this.attemptedAt=at;this.attempts++;this.lastError=null;}
    public void failed(Instant at,String error){this.attempts++;this.attemptedAt=at;this.status=this.attempts>=10?"FAILED":"PENDING";this.nextAttemptAt=at.plusSeconds(Math.min(3600L,30L<<(Math.min(this.attempts,7))));this.lastError=error.length()>500?error.substring(0,500):error;}
}
