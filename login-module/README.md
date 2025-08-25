# NexaBank Authentication Service

This module handles all user authentication and authorization for the NexaBank application.

## Features
- User registration and login
- JWT token generation and validation
- Session management
- Password security
- Role-based authentication (CUSTOMER, ADMIN, EMPLOYEE)

## Architecture
- **Backend**: Spring Boot application running on port 8080
- **Frontend**: React/Vite application running on port 5173
- **Database**: MySQL database `nexabank_auth`

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
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `POST /api/auth/logout` - User logout
- `GET /api/auth/me` - Get current user info
- `GET /api/auth/validate` - Validate token
- `POST /api/auth/refresh` - Refresh access token

## Integration
After successful authentication, users are redirected to the Customer Service module running on port 5174.

## Database Schema
The authentication service uses the following main tables:
- `users` - User account information
- `user_sessions` - Active user sessions and tokens

Run `banking_setup.sql` to set up the required database schema.
