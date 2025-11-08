package com.nexabank.customer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration for customer service
 * Creates required topics and configures Kafka settings
 */
@Configuration
public class KafkaConfig {
    
    @Value("${kafka.topic.alert:account.created}")
    private String alertTopic;
    
    /**
     * Creates the 'alert' topic if it doesn't exist
     * Partitions: 3 (for parallel processing)
     * Replicas: 1 (for local dev, increase for production)
     */
    @Bean
    public NewTopic alertTopic() {
        return TopicBuilder.name(alertTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
