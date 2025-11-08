# NexaBank Customer Module — API Documentation

This document provides comprehensive reference documentation for the Customer Module REST API. It includes endpoint specifications, request/response schemas, Kafka event documentation, and integration guidelines.

## Table of Contents
1. [Overview](#overview)
2. [Authentication & Security](#authentication--security)
3. [Health Endpoints](#health-endpoints)
4. [Customer Profile Management](#customer-profile-management)
5. [Kafka Events](#kafka-events)
6. [DTOs Reference](#dtos-reference)
7. [Integration Notes](#integration-notes)
8. [Setup & Configuration](#setup--configuration)
9. [Troubleshooting](#troubleshooting)

## Overview

**Base URL:** `http://localhost:1005/api`

**Modules:**
- Health checks and module information
- Customer profile CRUD operations with normalized schema
- Kafka event streaming for customer account events

**Controllers:**
- `HealthController` — `/api/customer/*`
- `UserProfileController` — `/api/profiles/*`

**Key Features:**
- Normalized database design (Customer + name components + identification + address components)
- JWT-based authentication and authorization
- Kafka event publishing for real-time notifications
- Cross-origin support for UI integration
- Comprehensive Swagger/OpenAPI documentation

---

## Authentication & Security

### Security Requirements
- **Protected Endpoints:** Require JWT bearer token in Authorization header
- **Public Endpoints:** `/api/profiles/public/*` endpoints (for inter-service communication)
- **Authorization Levels:**
  - `ADMIN` — Full access to all customer data
  - `CUSTOMER` — Access only to own profile data

### Authentication Header Format
```
Authorization: Bearer <JWT_TOKEN>
```

### CORS Configuration
Development: `http://localhost:3000, http://localhost:5173, http://localhost:5174, http://localhost:4200`

---

## Health Endpoints

### GET /api/customer/health
**Summary:** Basic health check endpoint

**Response:** `200 OK`
```json
{
  "status": "Customer Module is running",
  "module": "customer-module",
  "version": "1.0.0"
}
```

**Use Cases:**
- Kubernetes readiness/liveness probes
- CI/CD smoke tests
- Service discovery health checks

---

### GET /api/customer/info
**Summary:** Module metadata and information

**Response:** `200 OK`
```json
{
  "name": "NexaBank Customer Module",
  "description": "Customer Management and Banking Services",
  "entities": "Customer, BankAccount, Transaction, etc."
}
```

---

## Customer Profile Management

Base path: `/api/profiles`

### POST /api/profiles
**Summary:** Create new customer profile

**Description:**
Creates a new customer profile with normalized data structure. This endpoint:
- Creates main Customer record
- Stores name components in separate table
- Stores identification documents (Aadhar, PAN, Passport, DL)
- Stores address components
- **Publishes Kafka event to 'alert' topic** for downstream processing

**Request Body:** `CreateUserProfileRequest`
```json
{
  "userId": "user123",
  "email": "alice@example.com",
  "firstName": "Alice",
  "lastName": "Mitra",
  "middleName": "Kumar",
  "dateOfBirth": "1990-05-21",
  "gender": "FEMALE",
  "nationality": "Indian",
  "phoneNumber": "+919876543210",
  "alternatePhone": "+919876543211",
  "addressLine1": "12 Green Street",
  "addressLine2": "Apartment 4B",
  "city": "Mumbai",
  "state": "Maharashtra",
  "country": "India",
  "postalCode": "400001",
  "aadharNumber": "123412341234",
  "panNumber": "ABCDE1234F",
  "passportNumber": "M1234567",
  "drivingLicense": "DL-12345-678901",
  "occupation": "Software Engineer",
  "employerName": "NexaBank Tech",
  "annualIncome": 1200000.0
}
```

**Success Response:** `201 Created`
```json
{
  "profileId": "CUST-20251107-0001",
  "userId": "user123",
  "email": "alice@example.com",
  "firstName": "Alice",
  "lastName": "Mitra",
  "middleName": "Kumar",
  "dateOfBirth": "1990-05-21",
  "gender": "FEMALE",
  "nationality": "Indian",
  "phoneNumber": "+919876543210",
  "alternatePhone": "+919876543211",
  "addressLine1": "12 Green Street",
  "addressLine2": "Apartment 4B",
  "city": "Mumbai",
  "state": "Maharashtra",
  "country": "India",
  "postalCode": "400001",
  "maskedAadhar": "XXXX-XXXX-1234",
  "maskedPan": "XXXE1234F",
  "occupation": "Software Engineer",
  "employerName": "NexaBank Tech",
  "annualIncome": 1200000.0,
  "createdAt": "2025-11-07T14:30:00"
}
```

**Error Responses:**
- `400 Bad Request` — Invalid data or customer already exists
- `500 Internal Server Error` — Unexpected error

**Important Notes:**
- Kafka event publishing failure does NOT fail the request
- Customer number is auto-generated in format `CUST-YYYYMMDD-NNNNNN`
- Name and address components are stored in normalized tables
- Sensitive data (Aadhar, PAN) returned in masked format

---

### GET /api/profiles/user/{userId}
**Summary:** Get customer profile by user ID

**Path Parameters:**
- `userId` (string, required) — User ID from auth module

**Success Response:** `200 OK` — Returns `UserProfileResponse` (see POST response above)

**Error Responses:**
- `403 Forbidden` — Insufficient permissions
- `404 Not Found` — Customer profile not found
- `500 Internal Server Error`

**Use Cases:**
- Primary endpoint for inter-module communication
- Auth module resolving user metadata
- Transaction module getting customer details

---

### PUT /api/profiles/user/{userId}
**Summary:** Update customer profile

**Request Body:** Same as `CreateUserProfileRequest` (fields to update)

**Success Response:** `200 OK` — Returns updated `UserProfileResponse`

**Error Responses:**
- `400 Bad Request` — Invalid update data
- `403 Forbidden` — Insufficient permissions
- `404 Not Found` — Customer not found

**Implementation Notes:**
- Updates main customer record
- Deletes and recreates name components
- Does NOT publish Kafka event (only on creation)

---

### GET /api/profiles
**Summary:** List all customer profiles (Admin only)

**Success Response:** `200 OK` — Array of `UserProfileResponse`

**Error Responses:**
- `403 Forbidden` — Not an admin
- `500 Internal Server Error`

**Warning:** Not paginated in current implementation

---

### GET /api/profiles/search?name={name}
**Summary:** Search customers by name (Admin only)

**Query Parameters:**
- `name` (string) — Search string (firstName/lastName)

**Success Response:** `200 OK` — Array of matching `UserProfileResponse`

---

### DELETE /api/profiles/user/{userId}
**Summary:** Delete customer profile (Soft delete, Admin only)

**Success Response:** `200 OK` — Confirmation message

**Error Responses:**
- `403 Forbidden` — Not an admin
- `404 Not Found` — Customer not found

**Behavior:**
- Deletes related records (name components, identifications, address components)
- Performs soft delete on customer record

---

### GET /api/profiles/email/{email}
**Summary:** Get profile by email

**Path Parameters:**
- `email` (string, URL-encoded) — Customer email

**Success Response:** `200 OK` — Returns `UserProfileResponse`

**Error Responses:**
- `404 Not Found` — Customer not found
- `500 Internal Server Error`

**Notes:**
- Email parameter uses regex pattern `{email:.+}` to handle dots in email addresses
- Useful for account recovery and onboarding flows

---

### GET /api/profiles/public/customer/{customerNumber}/email
**Summary:** Get customer email by customer number (Public API)

**Description:** Public endpoint for inter-service communication (no JWT required)

**Path Parameters:**
- `customerNumber` (string) — e.g., `CUST-20251107-0001`

**Success Response:** `200 OK`
```json
{
  "customerNumber": "CUST-20251107-0001",
  "email": "alice@example.com"
}
```

**Error Response:** `404 Not Found`
```json
{
  "error": "Customer not found with customerNumber: CUST-20251107-999999"
}
```

**Use Cases:**
- Login module querying email for authentication
- Other microservices needing customer contact info

---

### GET /api/profiles/public/customer/{customerNumber}/phone
**Summary:** Get customer phone number by customer number (Public API)

**Description:** Public endpoint for inter-service communication (no JWT required). Returns both primary and alternate phone numbers.

**Path Parameters:**
- `customerNumber` (string, required) — e.g., `CUST-20251107-0001`

**Success Response:** `200 OK`
```json
{
  "customerNumber": "CUST-20251107-0001",
  "phoneNumber": "+919876543210",
  "alternatePhone": "+919876543211"
}
```

**Error Response:** `404 Not Found`
```json
{
  "error": "Customer not found with customerNumber: CUST-20251107-999999"
}
```

**Use Cases:**
- Notification service retrieving phone for SMS/OTP
- Customer support systems fetching contact information
- Alert systems sending notifications
- Inter-service communication for contact verification

---

## Kafka Events

### Overview
The Customer Module publishes events to Kafka for asynchronous processing by downstream services (notification, analytics, audit, etc.).

### Configuration
**Topic:** `alert`  
**Bootstrap Servers:** `localhost:9092` (configurable via `application.properties`)  
**Partitions:** 3  
**Replicas:** 1 (increase for production)

**application.properties:**
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
kafka.topic.alert=alert
```

---

### Event: CustomerAccountOpenedEvent

**Trigger:** When a new customer profile is created via `POST /api/profiles`

**Topic:** `alert`

**Event Schema:**
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
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

**Kafka Message Details:**
- **Key:** `customerNumber` (for partition assignment and ordering)
- **Value:** JSON-serialized `CustomerAccountOpenedEvent`
- **Headers:** None (JSON type headers disabled)

**Event Processing Flow:**
1. Customer profile created successfully in database
2. Event built from customer data and request
3. Event published asynchronously to Kafka
4. HTTP response returned immediately (doesn't wait for Kafka confirmation)
5. Kafka callback logs success/failure

**Error Handling:**
- Kafka publishing failures are logged but do NOT fail the API request
- Ensures customer creation succeeds even if Kafka is unavailable
- Async callbacks log partition, offset, and errors

**Downstream Consumers:**
- **Notification Service:** Send welcome email and SMS
- **Analytics Service:** Track customer onboarding metrics
- **Audit Service:** Log customer creation events
- **CRM System:** Sync customer data
- **Fraud Detection:** Analyze new account patterns

**Example Consumer (Spring Kafka):**
```java
@KafkaListener(topics = "alert", groupId = "notification-service")
public void handleCustomerAccountOpened(CustomerAccountOpenedEvent event) {
    // Send welcome email to event.getEmail()
    // Send SMS to event.getPhoneNumber()
    log.info("Processing account opened for customer: {}", event.getCustomerNumber());
}
```

---

## DTOs Reference

### CreateUserProfileRequest
**Purpose:** Request body for creating/updating customer profiles

**Fields:**
- `userId` (string) — From auth module
- `email` (string) — Customer email
- `firstName`, `lastName`, `middleName` (string) — Name components
- `dateOfBirth` (string, ISO date) — Format: `yyyy-MM-dd`
- `gender` (string) — e.g., MALE, FEMALE, OTHER
- `nationality` (string) — Default: "Indian"
- `phoneNumber` (string) — E.164 format recommended
- `alternatePhone` (string)
- `addressLine1`, `addressLine2` (string)
- `city`, `state`, `country`, `postalCode` (string)
- `aadharNumber`, `panNumber` (string) — Indian identity docs
- `passportNumber`, `drivingLicense` (string)
- `occupation`, `employerName` (string)
- `annualIncome` (number)

---

### UserProfileResponse
**Purpose:** API response containing customer profile data

**Additional Fields (vs Request):**
- `profileId` (string) — Same as customerNumber
- `customerNumber` (string) — Format: `CUST-YYYYMMDD-NNNNNN`
- `maskedAadhar` (string) — e.g., "XXXX-XXXX-1234"
- `maskedPan` (string) — e.g., "XXXE1234F"
- `createdAt` (datetime, ISO 8601)
- `customerType`, `customerStatus`, `kycStatus` (enum strings)

**Note:** Contains both masked and full values for backward compatibility

---

### CustomerAccountOpenedEvent
**Purpose:** Kafka event DTO for customer account opened

**Key Fields:**
- `eventId`, `eventType`, `eventTimestamp` — Event metadata
- `customerNumber`, `userId` — Identifiers
- `email`, `phoneNumber`, `alternatePhone` — **Primary contact info**
- `firstName`, `lastName`, `middleName`, `fullName` — Name details
- All personal, address, and professional details
- `source`, `remarks` — Event context

---

## Integration Notes

### Auth Module Integration
1. **Registration Flow:**
   - Auth module creates user account
   - Auth module calls `POST /api/profiles` to create customer profile
   - Customer module publishes Kafka event
   - Notification service sends welcome email/SMS

2. **Login Flow:**
   - Auth module can query `/api/profiles/user/{userId}` for customer details
   - Or query `/api/profiles/public/customer/{customerNumber}/email` without JWT

### UI Integration
**UI Path:** `ui/` folder (Vite + React app)

**Key Integration Points:**
- UI calls `/api/profiles/user/{userId}` to fetch profile after login
- Update profile via `PUT /api/profiles/user/{userId}`
- Admin pages use `/api/profiles` (list all) and `/api/profiles/search`

**Environment:**
- UI runs on different port (5173 by default)
- CORS enabled in controllers for development
- Production: Use reverse proxy

### Notification Service Integration
**Kafka Consumer Setup:**
```java
@KafkaListener(topics = "alert", groupId = "notification-service")
public void processCustomerEvent(CustomerAccountOpenedEvent event) {
    emailService.sendWelcomeEmail(event.getEmail(), event.getFullName());
    smsService.sendWelcomeSMS(event.getPhoneNumber(), event.getFirstName());
}
```

---

## Setup & Configuration

### Prerequisites
- Java 17+
- Maven 3.6+ (or use included `mvnw`)
- MySQL 8.0+
- Apache Kafka 3.0+ (for event streaming)
- Node.js 18+ (for UI)

### Backend Setup

**1. Clone and navigate:**
```bash
cd /Users/Jaiwant/repos/team-5-bt-lab/customer-module
```

**2. Configure database (application.properties):**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/nexabank_customer
spring.datasource.username=root
spring.datasource.password=root@fintech
```

**3. Start Kafka (if not running):**
```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka broker
bin/kafka-server-start.sh config/server.properties
```

**4. Build and run:**
```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

Server starts on: `http://localhost:1005`

**5. Verify health:**
```bash
curl http://localhost:1005/api/customer/health
```

### Frontend Setup

```bash
cd ui
npm install
npm run dev
```

UI starts on: `http://localhost:5173`

### Kafka Monitoring

**View topic messages:**
```bash
bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic alert \
  --from-beginning \
  --property print.key=true \
  --property print.value=true
```

**Check topic details:**
```bash
bin/kafka-topics.sh --describe --topic alert --bootstrap-server localhost:9092
```

---

## Troubleshooting

### Common Issues

**1. Kafka Connection Failed**
- **Symptom:** Logs show "Failed to publish customer account opened event"
- **Solution:** 
  - Verify Kafka is running: `jps | grep Kafka`
  - Check `spring.kafka.bootstrap-servers` in `application.properties`
  - Customer creation still succeeds (Kafka errors are non-blocking)

**2. 400 Bad Request on Profile Creation**
- **Cause:** Customer already exists for userId
- **Solution:** Check database or use different userId

**3. 404 Not Found on GET requests**
- **Cause:** Incorrect userId, customerNumber, or email
- **Solution:** Verify identifier matches database records

**4. CORS Errors in Browser**
- **Cause:** UI running on different port
- **Solution:** 
  - Verify backend CORS config includes UI URL
  - Check `app.cors.allowed-origins` in `application.properties`

**5. Kafka Event Not Received by Consumer**
- **Cause:** Topic mismatch, serialization issues, or consumer not running
- **Solution:**
  - Verify topic name: `alert`
  - Check consumer group ID
  - Use kafka-console-consumer to verify messages are published

**6. Database Connection Error**
- **Cause:** MySQL not running or wrong credentials
- **Solution:**
  - Start MySQL: `brew services start mysql` (macOS)
  - Verify credentials in `application.properties`

---

## API Testing Examples

### Create Customer with curl
```bash
curl -X POST http://localhost:1005/api/profiles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "userId": "user789",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1985-03-15",
    "gender": "MALE",
    "phoneNumber": "+919123456789",
    "city": "Mumbai",
    "state": "Maharashtra",
    "country": "India"
  }'
```

### Get Customer by User ID
```bash
curl -X GET http://localhost:1005/api/profiles/user/user789 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Phone Number (Public API)
```bash
curl -X GET http://localhost:1005/api/profiles/public/customer/CUST-20251107-0001/phone
```

---

## Next Steps & Recommendations

### Production Readiness
1. **Kafka Configuration:**
   - Increase replicas to 3 for fault tolerance
   - Configure retention policy
   - Enable compression
   - Add monitoring (Kafka Manager, Confluent Control Center)

2. **API Improvements:**
   - Add pagination to list endpoints
   - Implement rate limiting
   - Add request validation with `@Valid`
   - Return structured error responses (JSON with code/message/details)

3. **Security:**
   - Remove unmasked PII from responses
   - Implement field-level encryption for sensitive data
   - Add audit logging
   - Configure proper CORS for production

4. **Observability:**
   - Add distributed tracing (Spring Cloud Sleuth + Zipkin)
   - Implement metrics (Micrometer + Prometheus)
   - Set up alerts for Kafka publishing failures
   - Add health checks for Kafka connectivity

5. **Documentation:**
   - Generate OpenAPI YAML from annotations
   - Add Postman collection
   - Create architecture diagrams

---

## Support & Resources

- **Swagger UI:** http://localhost:1005/swagger-ui.html
- **OpenAPI Spec:** http://localhost:1005/api-docs
- **Health Check:** http://localhost:1005/api/customer/health

For additional support, refer to the project README.md or contact the development team.

---

**Last Updated:** November 7, 2025  
**Version:** 1.0.0  
**Module:** customer-module
