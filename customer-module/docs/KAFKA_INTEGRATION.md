# Kafka Integration - Quick Reference

## Overview
When a customer opens an account (creates a profile), a Kafka event is automatically published to the `alert` topic for downstream processing by notification services, analytics, audit systems, etc.

## Event Flow

```
POST /api/profiles (Create Customer)
    ↓
Customer saved to database
    ↓
Kafka event published to "alert" topic
    ↓
[Event contains: email, phone, customer details]
    ↓
Downstream consumers process event
    ↓
- Welcome email sent
- SMS notification sent
- Analytics updated
- Audit log created
```

## Configuration

### application.properties
```properties
# Kafka Bootstrap Servers
spring.kafka.bootstrap-servers=localhost:9092

# Producer Configuration
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.properties.spring.json.add.type.headers=false

# Topic Name
kafka.topic.alert=alert
```

### Topic Details
- **Name:** `alert`
- **Partitions:** 3
- **Replicas:** 1 (increase for production)
- **Key:** customerNumber (for ordering and partition assignment)
- **Value:** JSON-serialized CustomerAccountOpenedEvent

## Event Schema

### CustomerAccountOpenedEvent
```json
{
  "eventId": "uuid",
  "eventType": "CUSTOMER_ACCOUNT_OPENED",
  "eventTimestamp": "2025-11-07T14:30:00",
  
  "customerNumber": "CUST-20251107-0001",
  "userId": "user123",
  
  "email": "alice@example.com",
  "phoneNumber": "+919876543210",
  "alternatePhone": "+919876543211",
  
  "firstName": "Alice",
  "lastName": "Mitra",
  "middleName": "Kumar",
  "fullName": "Alice Kumar Mitra",
  
  "dateOfBirth": "1990-05-21",
  "gender": "FEMALE",
  "nationality": "Indian",
  
  "addressLine1": "12 Green Street",
  "addressLine2": "Apartment 4B",
  "city": "Mumbai",
  "state": "Maharashtra",
  "country": "India",
  "postalCode": "400001",
  
  "occupation": "Software Engineer",
  "employerName": "NexaBank Tech",
  "annualIncome": 1200000.0,
  
  "source": "API",
  "remarks": "Customer profile created via REST API"
}
```

## Key Components

### 1. Event DTO
**File:** `CustomerAccountOpenedEvent.java`
- Contains all customer details
- **Highlights email and phone number** for alert processing
- Uses Lombok @Builder for easy construction
- Jackson annotations for JSON serialization

### 2. Event Publisher Service
**File:** `CustomerEventPublisher.java`
- Publishes events asynchronously
- Uses customerNumber as message key
- Logs success/failure with partition and offset
- Provides sync publishing option if needed

### 3. Kafka Configuration
**File:** `KafkaConfig.java`
- Auto-creates `alert` topic if missing
- Configures partitions and replicas
- Uses properties from application.properties

### 4. Controller Integration
**File:** `UserProfileController.java`
- Publishes event after customer creation
- **Non-blocking:** Kafka failures don't affect customer creation
- Logs Kafka errors but continues request processing

## Consumer Example

### Notification Service (Java/Spring)
```java
@Service
public class CustomerNotificationConsumer {
    
    @KafkaListener(topics = "alert", groupId = "notification-service")
    public void handleCustomerAccountOpened(CustomerAccountOpenedEvent event) {
        log.info("Customer account opened: {}", event.getCustomerNumber());
        
        // Send welcome email
        emailService.sendWelcomeEmail(
            event.getEmail(),
            event.getFullName(),
            event.getCustomerNumber()
        );
        
        // Send SMS notification
        smsService.sendWelcomeSMS(
            event.getPhoneNumber(),
            event.getFirstName()
        );
        
        // Log analytics
        analyticsService.trackCustomerRegistration(event);
    }
}
```

### Analytics Service (Python)
```python
from kafka import KafkaConsumer
import json

consumer = KafkaConsumer(
    'alert',
    bootstrap_servers='localhost:9092',
    group_id='analytics-service',
    value_deserializer=lambda m: json.loads(m.decode('utf-8'))
)

for message in consumer:
    event = message.value
    if event['eventType'] == 'CUSTOMER_ACCOUNT_OPENED':
        print(f"New customer: {event['customerNumber']}")
        print(f"Email: {event['email']}")
        print(f"Phone: {event['phoneNumber']}")
        # Process analytics
```

## Testing Kafka Integration

### 1. Start Kafka
```bash
# Start Zookeeper
cd /path/to/kafka
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka (separate terminal)
bin/kafka-server-start.sh config/server.properties
```

### 2. Monitor Events
```bash
# Console consumer to see published events
bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic alert \
  --from-beginning \
  --property print.key=true \
  --property print.value=true \
  --property print.timestamp=true
```

### 3. Create Test Customer
```bash
curl -X POST http://localhost:1005/api/profiles \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "testuser123",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "phoneNumber": "+919999999999",
    "city": "Mumbai",
    "state": "Maharashtra",
    "country": "India"
  }'
```

### 4. Verify Event Published
Check the console consumer output - you should see:
```
Key: CUST-20251107-0001
Value: {"eventId":"...","eventType":"CUSTOMER_ACCOUNT_OPENED",...}
```

## Error Handling

### Kafka Unavailable
- **Behavior:** Customer creation succeeds, Kafka error logged
- **Log Message:** "Failed to publish customer account opened event"
- **Action:** Event lost, manual recovery may be needed

### Production Recommendations:
1. **Dead Letter Queue:** Configure DLQ for failed events
2. **Retry Logic:** Add retry mechanism with exponential backoff
3. **Monitoring:** Set up alerts for Kafka publishing failures
4. **Event Store:** Store events in database before/after Kafka publish

## Monitoring

### Application Logs
```bash
# Watch for Kafka events
tail -f logs/application.log | grep "Publishing customer account opened event"
```

### Kafka Topic Info
```bash
# List topics
bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# Describe alert topic
bin/kafka-topics.sh --describe --topic alert --bootstrap-server localhost:9092

# Check consumer lag
bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group notification-service
```

## Troubleshooting

### Issue: Events not published
**Symptoms:**
- No logs showing "Publishing customer account opened event"
- Console consumer shows no new messages

**Solutions:**
1. Check Kafka is running: `jps | grep Kafka`
2. Verify `spring.kafka.bootstrap-servers` configuration
3. Check application logs for connection errors
4. Ensure `CustomerEventPublisher` bean is autowired

### Issue: Consumer not receiving events
**Solutions:**
1. Verify topic name matches: `alert`
2. Check consumer group ID is unique
3. Ensure proper deserialization configuration
4. Check consumer is subscribed to correct topic

### Issue: Serialization errors
**Solutions:**
1. Verify `CustomerAccountOpenedEvent` has proper Jackson annotations
2. Check `spring.kafka.producer.value-serializer` is set to `JsonSerializer`
3. Ensure no circular references in event DTO

## Production Checklist

- [ ] Increase Kafka replicas to 3
- [ ] Configure retention policy (e.g., 7 days)
- [ ] Enable compression (snappy or lz4)
- [ ] Set up monitoring (Kafka Manager, Prometheus, Grafana)
- [ ] Configure consumer group management
- [ ] Implement idempotent consumers
- [ ] Add dead letter queue
- [ ] Set up alerts for lag and failures
- [ ] Document consumer services and their purposes
- [ ] Test failover scenarios

## References

- **Full API Documentation:** [docs/API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
- **Spring Kafka Docs:** https://spring.io/projects/spring-kafka
- **Apache Kafka Docs:** https://kafka.apache.org/documentation/

---

**Last Updated:** November 7, 2025
