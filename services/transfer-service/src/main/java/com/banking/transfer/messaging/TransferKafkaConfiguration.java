package com.banking.transfer.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TransferKafkaConfiguration {
    @Bean
    NewTopic transfersEventsTopic() { return TopicBuilder.name("transfers.events.v1").partitions(3).replicas(1).build(); }
}
