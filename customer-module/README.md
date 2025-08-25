# NexaBank Customer Service

This module handles all customer-related functionality including banking operations, account management, and dashboards.

## Features
- Customer dashboards with real account data
- Admin dashboards for account and user management
- Bank account creation and management (Savings, Current, Fixed Deposit, Loan)
- Transaction history and processing
- Account balance management
- Role-based access control

## Architecture
- **Backend**: Spring Boot application running on port 8081
- **Frontend**: React/Vite application running on port 5174
- **Database**: MySQL database `nexabank_customer`

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

### Backend Setup
1. Navigate to the root directory of this module
2. Make sure MySQL is running
3. Update database credentials in `src/main/resources/application.properties`
4. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

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

## Data Policy
- All displayed data is real and comes from the database
- No sample/mock data is used in production components
- Fake notification badges and placeholder features have been removed
