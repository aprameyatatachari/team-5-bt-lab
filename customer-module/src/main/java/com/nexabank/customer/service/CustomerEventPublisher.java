package com.nexabank.customer.service;

import com.nexabank.customer.dto.CustomerAccountOpenedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing customer-related events to Kafka
 * Handles asynchronous event publishing with error handling and logging
 */
@Service
public class CustomerEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerEventPublisher.class);
    
    @Autowired
    private KafkaTemplate<String, CustomerAccountOpenedEvent> kafkaTemplate;
    
    @Value("${kafka.topic.alert:alert}")
    private String alertTopic;
    
    /**
     * Publishes a customer account opened event to the alert topic
     * 
     * @param event The customer account opened event
     * @return CompletableFuture for async handling
     */
    public CompletableFuture<SendResult<String, CustomerAccountOpenedEvent>> publishAccountOpenedEvent(
            CustomerAccountOpenedEvent event) {
        
        logger.info("Publishing customer account opened event for customer: {} to topic: {}", 
                event.getCustomerNumber(), alertTopic);
        
        try {
            // Send event with customer number as key (for partitioning)
            CompletableFuture<SendResult<String, CustomerAccountOpenedEvent>> future = 
                kafkaTemplate.send(alertTopic, event.getCustomerNumber(), event);
            
            // Add callback for success/failure handling
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Successfully published event for customer: {} to partition: {} with offset: {}",
                            event.getCustomerNumber(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish event for customer: {} - Error: {}", 
                            event.getCustomerNumber(), ex.getMessage(), ex);
                }
            });
            
            return future;
            
        } catch (Exception e) {
            logger.error("Exception while publishing customer account opened event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish customer event to Kafka", e);
        }
    }
    
    /**
     * Publishes event synchronously (waits for confirmation)
     * Use only when you need to ensure event is published before proceeding
     * 
     * @param event The customer account opened event
     */
    public void publishAccountOpenedEventSync(CustomerAccountOpenedEvent event) {
        try {
            CompletableFuture<SendResult<String, CustomerAccountOpenedEvent>> future = 
                publishAccountOpenedEvent(event);
            
            // Wait for completion (blocking)
            SendResult<String, CustomerAccountOpenedEvent> result = future.get();
            logger.info("Synchronous publish completed for customer: {}", event.getCustomerNumber());
            
        } catch (Exception e) {
            logger.error("Synchronous publish failed for customer: {} - Error: {}", 
                    event.getCustomerNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish customer event synchronously", e);
        }
    }
}
