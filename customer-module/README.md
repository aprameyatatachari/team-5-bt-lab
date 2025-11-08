# NexaBank Customer Service

This module handles all customer-related functionality including banking operations, account management, and dashboards.

## Features
- Customer dashboards with real account data
- Admin dashboards for account and user management
- Bank account creation and management (Savings, Current, Fixed Deposit, Loan)
- Transaction history and processing
- Account balance management
- Role-based access control
- **Kafka event streaming** for customer account opened events
- Public APIs for inter-service communication

## Architecture
- **Backend**: Spring Boot application running on port 1005
- **Frontend**: React/Vite application running on port 5173
- **Database**: MySQL database `nexabank_customer`
- **Message Broker**: Apache Kafka for event streaming (port 9092)
- **Event Topics**: `alert` - Customer account opened events

## User Roles & Features

### Customer Dashboard
- View account summaries with real balances
- Transaction history
- Create new accounts (savings, fixed deposit, etc.)
- Account management
- Banking services access

### Admin Dashboard
- User management (create, edit, activate/deactivate users)
- Account management across all customers
- Banking statistics and analytics
- System administration features

### Employee Dashboard
- Similar to admin dashboard with limited permissions
- Customer support features
- Transaction assistance

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+ (or use included `mvnw`)
- MySQL 8.0+
- Apache Kafka 3.0+ (for event streaming)
- Node.js 18+ (for UI)

### Backend Setup
1. **Start Kafka** (required for customer account events):
   ```bash
   # Start Zookeeper
   bin/zookeeper-server-start.sh config/zookeeper.properties
   
   # Start Kafka (in separate terminal)
   bin/kafka-server-start.sh config/server.properties
   ```

2. Navigate to the root directory of this module
3. Make sure MySQL is running
4. Update database credentials in `src/main/resources/application.properties`
5. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   
   The application will start on `http://localhost:1005`

### Frontend Setup
1. Navigate to the `ui` directory
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```

## API Endpoints

### Account Management
- `GET /api/accounts/my-accounts` - Get user accounts
- `POST /api/accounts/create` - Create new account
- `GET /api/accounts/{id}/transactions` - Get account transactions

### Admin Operations
- `GET /api/admin/users` - Get all users
- `PUT /api/admin/users/{id}` - Update user
- `GET /api/admin/stats` - Get banking statistics

### Transactions
- `GET /api/transactions/my-transactions` - Get user transactions
- `POST /api/transactions/transfer` - Process money transfer

## Authentication
This module requires valid JWT tokens from the Authentication Service. Users must login through the auth service first.

## Database Schema
The customer service uses the following main tables:
- `bank_accounts` - Customer bank accounts
- `transactions` - All banking transactions
- User data is synchronized from the auth service

Run `banking_setup.sql` to set up the required database schema.

## Kafka Event Streaming

### Customer Account Opened Event
When a customer creates a profile (`POST /api/profiles`), an event is automatically published to the `alert` Kafka topic containing:
- **Customer details**: name, userId, customerNumber
- **Contact information**: email, phone number (primary and alternate)
- **Address and personal details**: for notifications and analytics

**Event Processing Flow:**
```
Customer Created → Kafka Event Published → Downstream Consumers
                                         ↓
                      Notification Service (Welcome Email/SMS)
                      Analytics Service (Track Registrations)
                      Audit Service (Log Events)
                      CRM Systems (Sync Customer Data)
```

### Monitoring Kafka Events
```bash
# Monitor events in real-time
bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic alert \
  --from-beginning
```

**For detailed Kafka integration documentation, see:** [docs/KAFKA_INTEGRATION.md](./docs/KAFKA_INTEGRATION.md)

## Documentation

- **[API Documentation](./docs/API_DOCUMENTATION.md)** - Complete REST API reference with examples
- **[Kafka Integration](./docs/KAFKA_INTEGRATION.md)** - Event streaming setup and consumer examples
- **[Swagger UI](http://localhost:1005/swagger-ui.html)** - Interactive API documentation (when running)

## Data Policy
- All displayed data is real and comes from the database
- No sample/mock data is used in production components
- Fake notification badges and placeholder features have been removed
- Kafka events are published asynchronously (non-blocking)
