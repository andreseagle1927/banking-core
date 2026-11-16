package com.banking.notification;
import com.banking.platform.security.ResourceServerSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication @Import(ResourceServerSecurity.class) @EnableKafka @EnableScheduling public class NotificationServiceApplication { public static void main(String[] args) { SpringApplication.run(NotificationServiceApplication.class, args); } }
