package com.banking.notification.application;

import com.banking.notification.infrastructure.persistence.NotificationDeliveryEntity;
import com.banking.notification.infrastructure.persistence.NotificationDeliveryJpaRepository;
import java.time.Instant;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationEmailDeliveryWorker {
    private static final Logger log=LoggerFactory.getLogger(NotificationEmailDeliveryWorker.class);
    private final NotificationDeliveryJpaRepository deliveries;private final JavaMailSender mail;private final String from;
    public NotificationEmailDeliveryWorker(NotificationDeliveryJpaRepository deliveries,JavaMailSender mail,
            @org.springframework.beans.factory.annotation.Value("${banking.notifications.from:no-reply@core-banking.local}") String from){this.deliveries=deliveries;this.mail=mail;this.from=from;}
    @Scheduled(fixedDelayString="${banking.notifications.poll-ms:30000}",initialDelayString="10000")
    @SchedulerLock(name = "notificationEmailDelivery", lockAtMostFor = "10m")
    @Transactional
    public void deliverPending(){
        for(NotificationDeliveryEntity delivery:deliveries.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc("PENDING",Instant.now())){
            try{
                SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(delivery.getRecipientEmail());
                message.setSubject(delivery.getSubject());message.setText(delivery.getMessage());mail.send(message);
                delivery.sent(Instant.now());
            }catch(RuntimeException ex){delivery.failed(Instant.now(),"SMTP delivery failed: "+ex.getClass().getSimpleName());
                log.warn("Notification email delivery failed; delivery will retry if attempts remain");}
        }
    }
}
