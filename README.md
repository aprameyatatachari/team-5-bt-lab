# NexaBank 
# NexaBank Authentication & Application Flow Documentation

## 📋 Table of Contents
1. [System Architecture Overview](#system-architecture-overview)
2. [Module Structure](#module-structure)
3. [Authentication Flow](#authentication-flow)
4. [Session Management](#session-management)
5. [Security Mechanisms](#security-mechanisms)
6. [User Registration Flow](#user-registration-flow)
7. [Token Management](#token-management)
8. [Logout Process](#logout-process)
9. [OTP-Based 2FA Flow (Planned)](#otp-based-2fa-flow)
10. [Database Schema](#database-schema)

---

## System Architecture Overview

NexaBank is a microservices-based banking application consisting of two primary modules:

### **Login Module (Port 8080)**
Handles all authentication, authorization, and user management operations. This is the central authentication service that other modules depend on for security.

### **Customer Module (Port 8081)**
Manages customer profile information including personal details, identification documents, and name components. This module has been cleaned up to focus solely on customer profile data (no banking operations).

### **Technology Stack**
- **Backend**: Spring Boot 3.x with Java 17
- **Frontend**: React with TypeScript and Vite
- **Database**: MySQL 8.0
- **Cache/Session Store**: Redis
- **Authentication**: JWT (JSON Web Tokens) with RS256 signing
- **Security**: BCrypt password hashing, JWT denylist, rate limiting

---

## Module Structure

### Login Module Components

**Controllers:**
- `AuthController` - Handles login, registration, logout, token refresh, and validation

**Services:**
- `UserService` - User CRUD operations and authentication
- `JwtTokenService` - JWT generation, validation, and denylist management
- `RedisSessionService` - Session tracking and lockout management
- `OtpService` - OTP generation and verification (planned)
- `OtplessService` - Integration with OTPless API (planned)

**Entities:**
- `User` - Core user authentication data (email, password, roles, status)
- `OtpVerification` - OTP tracking and validation (planned)

**Security:**
- `SecurityConfig` - JWT filter chain and endpoint security
- `JwtAuthenticationFilter` - Token validation on each request

### Customer Module Components

**Controllers:**
- `UserProfileController` - Customer profile CRUD operations

**Services:**
- `CustomerService` - Customer profile management
- `CustomerNameComponentService` - Name management (first, middle, last)
- `CustomerIdentificationService` - ID documents (Aadhar, PAN, Passport, License)

**Entities:**
- `Customer` - Core customer profile data
- `CustomerNameComponent` - Normalized name storage
- `CustomerIdentification` - Identity document storage

---

## Authentication Flow

### **Step 1: User Initiates Login**
User enters email and password on the React frontend login form. The form validates input format before sending the request.

### **Step 2: Login Request Sent**
Frontend makes POST request to `/api/auth/login` with email and password in JSON format.

### **Step 3: Lockout Check**
Backend checks Redis to see if the user is currently locked out. If locked out, returns 423 LOCKED status with remaining lockout time in seconds.

### **Step 4: Password Verification**
Backend retrieves user from MySQL database by email. BCrypt compares the provided password with the stored hashed password.

### **Step 5: JWT Token Generation**
If authentication succeeds:
- Generates Access Token (24-hour expiry) with user claims (userId, email, roles)
- Generates Refresh Token (7-day expiry) for token renewal
- Both tokens include JTI (JWT ID) for tracking and revocation

### **Step 6: Session Lockout Applied**
Backend sets a 10-minute lockout in Redis for the user's email. This prevents the same user from logging in again from another device/session without explicitly logging out first.

### **Step 7: Session Creation**
Redis stores the JTI-to-userId mapping for session tracking. This allows the system to invalidate specific sessions.

### **Step 8: Response Sent**
Backend returns 200 OK with:
- Access Token
- Refresh Token
- Token Type (Bearer)
- Expiry time (86400 seconds = 24 hours)
- User object (userId, email, roles, status)

### **Step 9: Frontend Token Storage**
Frontend stores tokens in memory (Context API) and makes them available to all authenticated components. Tokens are NOT stored in localStorage to prevent XSS attacks.

### **Step 10: Authenticated Requests**
All subsequent API requests include the Access Token in the Authorization header as "Bearer {token}".

---

## Session Management

### Bank-Style Session Control
NexaBank implements a unique session control mechanism:

**10-Minute Lockout After Login:**
- When a user logs in successfully, their email is locked for 10 minutes in Redis
- During this period, the same user CANNOT login again from another device
- This prevents session hijacking and enforces single-session usage
- The lockout is ONLY cleared when the user explicitly logs out

**Why This Approach?**
- Prevents multiple simultaneous sessions for the same user
- Enhances security by ensuring users log out properly
- Mimics physical bank token systems where you can't have two tokens

**Session Tracking:**
- Each JWT has a unique JTI (JWT ID) stored in Redis
- The system knows which specific session/token belongs to which user
- When logout occurs, the specific JTI is invalidated (added to denylist)

---

## Security Mechanisms

### JWT Token Security

**Token Denylist (Blacklist):**
- Redis stores invalidated JWT IDs (JTIs) with their expiry times
- When a user logs out, their token JTI is added to the denylist
- Every API request checks if the token's JTI is on the denylist
- Denylisted tokens are automatically removed after expiry to save memory

**Token Structure:**
- **Header**: Algorithm (RS256), Token Type (JWT)
- **Payload**: userId, email, roles, issued-at (iat), expiration (exp), JWT ID (jti)
- **Signature**: RSA-signed with private key, verified with public key

**Token Validation Flow:**
1. Extract token from Authorization header
2. Check if JTI is on denylist (immediate rejection if yes)
3. Verify signature using public key
4. Check expiration time
5. Extract user claims
6. Verify user still exists and is ACTIVE in database

### Password Security

**BCrypt Hashing:**
- All passwords are hashed using BCrypt with strength 10
- BCrypt automatically generates salt for each password
- Password verification compares plaintext with hashed value
- Even identical passwords produce different hashes due to unique salts

**Password Validation Rules:**
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character from: `!@#$%^&*()_+-=[]{}|;':",./<>?`

### Rate Limiting

**Redis-Based Rate Limiting:**
- OTP requests: Maximum 3 per 10 minutes per user
- OTP resend: 60-second cooldown between resends
- Failed login attempts tracked (planned feature)

---

## User Registration Flow

### **Step 1: User Fills Registration Form**
React form collects:
- Personal: First name, middle name, last name, date of birth, gender
- Contact: Email, phone number, address, city, state, postal code
- Identity: Aadhar number, PAN number
- Authentication: Password

### **Step 2: Frontend Validation**
Zod schema validates all fields before submission:
- Email format validation
- Phone: 10 digits starting with 6-9
- Aadhar: 12 digits, cannot start with 0 or 1
- PAN: Format AAAAA9999A
- Password: Complex requirements
- Date of Birth: Must be 18+ years old
- Postal Code: 6 digits, cannot start with 0

### **Step 3: Registration Request**
POST request to `/api/auth/register` with all validated data.

### **Step 4: Backend Validation**
Bean Validation annotations check:
- Email uniqueness
- Phone number format
- Aadhar/PAN format compliance
- Password strength
- Age verification (18+)

### **Step 5: User Creation**
**In Login Module:**
- Creates User entity with email, hashed password, roles (CUSTOMER), status (ACTIVE)
- Stores in MySQL users table
- Generates unique userId (UUID)

### **Step 6: Customer Profile Creation**
**Cross-Module Communication:**
- Login module makes HTTP POST to Customer module at `http://localhost:8081/api/profiles`
- Includes JWT token in Authorization header for authentication
- Sends complete user profile data

**In Customer Module:**
- Creates Customer entity with userId, email, phone, DOB, gender, address
- Creates CustomerNameComponent entries for first, middle, last names
- Creates CustomerIdentification entries for Aadhar and PAN documents
- All stored with foreign key relationships to Customer

### **Step 7: Immediate Auto-Login**
After successful registration:
- Backend generates JWT tokens automatically
- Returns Access Token and Refresh Token
- User is logged in immediately without needing to login again

### **Step 8: Welcome Response**
Frontend receives 201 CREATED with tokens and user data, automatically navigates to dashboard.

---

## Token Management

### Access Token
**Purpose:** Short-lived token for API authentication
**Expiry:** 24 hours (86400 seconds)
**Usage:** Included in every API request header
**Refresh:** Must be refreshed before expiry using Refresh Token

### Refresh Token
**Purpose:** Long-lived token to obtain new Access Tokens
**Expiry:** 7 days (604800 seconds)
**Usage:** Only used at `/api/auth/refresh` endpoint
**Security:** Single-use (invalidated after refresh)

### Token Refresh Flow

**Step 1:** Frontend detects Access Token is about to expire (typically 5 minutes before expiry)

**Step 2:** Makes POST request to `/api/auth/refresh` with current Refresh Token

**Step 3:** Backend validates Refresh Token:
- Checks if not on denylist
- Verifies signature
- Checks expiration
- Verifies user is still locked out (session still active)

**Step 4:** If valid, generates NEW Access Token and NEW Refresh Token

**Step 5:** OLD Refresh Token is added to denylist (single-use enforcement)

**Step 6:** Resets the 10-minute lockout timer (session extended)

**Step 7:** Returns new tokens to frontend

**Step 8:** Frontend updates tokens in memory and continues operation seamlessly

---

## Logout Process

### **Step 1: User Clicks Logout**
Frontend sends POST request to `/api/auth/logout` with current Access Token in Authorization header.

### **Step 2: Token Denylist**
Backend extracts JTI from token and adds it to Redis denylist with expiry timestamp. This immediately invalidates the token.

### **Step 3: Lockout Removal**
Backend extracts user email from token and removes the 10-minute lockout from Redis. This allows the user to login again immediately.

### **Step 4: Session Invalidation**
Backend removes the JTI-to-userId mapping from Redis, destroying the session record.

### **Step 5: Confirmation**
Backend returns 200 OK with success message.

### **Step 6: Frontend Cleanup**
Frontend clears tokens from memory, resets authentication context, and redirects to login page.

### **Result:**
- User can login again immediately (no 10-minute wait)
- Old tokens are completely invalidated
- Session is cleanly destroyed

---

## OTP-Based 2FA Flow (Planned Implementation)

### Integration with OTPless

**Why OTPless?**
- WhatsApp-based OTP delivery (higher success rate in India)
- Automatic SMS fallback if WhatsApp fails
- Cost-effective (₹0.10-0.15 per verification)
- 25 free verifications per month
- Better user experience than SMS-only solutions

### Modified Login Flow with OTP

**Step 1: Email/Password Authentication**
User enters email and password, backend validates credentials.

**Step 2: OTP Generation**
Instead of immediately issuing JWT tokens:
- Backend generates 6-digit OTP
- Sends OTP to user's mobile via OTPless (WhatsApp preferred, SMS fallback)
- Returns TEMPORARY token (5-minute expiry, only valid for OTP verification)
- User session is NOT yet created

**Step 3: OTP Delivery**
OTPless delivers OTP via WhatsApp message or SMS. User receives OTP on their registered mobile number.

**Step 4: User Enters OTP**
Frontend shows OTP input screen with 6-digit input boxes and countdown timer (5 minutes).

**Step 5: OTP Verification**
User submits OTP code, frontend sends POST to `/api/auth/verify-otp` with:
- Temporary token
- OTP code entered by user

**Step 6: OTP Validation**
Backend:
- Validates temporary token
- Checks OTP against OTPless verification API
- Verifies OTP hasn't expired (5 minutes)
- Checks attempt count (max 3 attempts)
- Updates OTP status in database

**Step 7: Success - Issue Real JWT Tokens**
If OTP is correct:
- Generate Access Token and Refresh Token
- Set 10-minute session lockout
- Create Redis session
- Return tokens to frontend
- User is fully logged in

**Step 8: Failure Handling**
If OTP is incorrect:
- Increment attempt counter
- If under 3 attempts: allow retry
- If 3 failed attempts: invalidate OTP, require new login

### OTP Resend Flow

**Cooldown Period:** 60 seconds between resend requests

**Step 1:** User clicks "Resend OTP" button

**Step 2:** Frontend checks if 60 seconds have passed since last send

**Step 3:** Backend checks:
- Maximum 3 resends per OTP session
- 60-second cooldown enforced via Redis
- Original OTP session still valid

**Step 4:** If allowed, OTPless sends new OTP (extends expiry to 5 minutes from now)

**Step 5:** Frontend shows countdown timer and confirmation message

### OTP Database Tracking

**OtpVerification Table Stores:**
- User ID
- Phone number
- OTP code
- Order ID (from OTPless)
- Status (PENDING, VERIFIED, EXPIRED, FAILED, MAX_ATTEMPTS_REACHED)
- Created timestamp
- Expiry timestamp (5 minutes from creation)
- Verification timestamp (when successfully verified)
- Attempt count (max 3)
- Resend count (max 3)

**Benefits of Database Tracking:**
- Audit trail of all OTP operations
- Rate limiting enforcement
- Analytics on OTP success rates
- Fraud detection capabilities

---

## Database Schema

### Login Module Tables

**users:**
- user_id (VARCHAR, PK, UUID)
- email (VARCHAR, UNIQUE)
- password_hash (VARCHAR, BCrypt)
- user_type (ENUM: CUSTOMER, ADMIN)
- roles (VARCHAR, comma-separated)
- status (ENUM: ACTIVE, INACTIVE, SUSPENDED)
- created_at, updated_at
- phone_number (VARCHAR, for OTP)

**otp_verifications:** (planned)
- id (BIGINT, PK, Auto-increment)
- user_id (VARCHAR, FK to users)
- phone_number (VARCHAR)
- otp_code (VARCHAR)
- order_id (VARCHAR, OTPless tracking)
- status (ENUM)
- created_at, expires_at, verified_at
- attempt_count, resend_count

### Customer Module Tables

**customers:**
- customer_id (BIGINT, PK, Auto-increment)
- user_id (VARCHAR, FK to users in login module)
- email_id (VARCHAR)
- phone_number (VARCHAR)
- date_of_birth (DATE)
- gender (ENUM: MALE, FEMALE, OTHER)
- address_line1, address_line2, city, state, postal_code
- created_at, updated_at

**customer_name_components:**
- id (BIGINT, PK)
- customer_id (BIGINT, FK to customers)
- name_component_type (ENUM: FIRST_NAME, MIDDLE_NAME, LAST_NAME)
- name_value (VARCHAR)
- sequence_number (INT, for ordering)

**customer_identification:**
- id (BIGINT, PK)
- customer_id (BIGINT, FK to customers)
- identification_item (ENUM: AADHAR_CARD, PAN_CARD, PASSPORT, DRIVING_LICENSE)
- identification_number (VARCHAR)
- issue_date, expiry_date
- issuing_authority (VARCHAR)

---

## Inter-Module Communication

### How Login Module Talks to Customer Module

**Scenario:** User registration requires creating both auth user and customer profile

**Step 1:** Login module creates User in its own database

**Step 2:** Login module makes HTTP POST to Customer module:
```
POST http://localhost:8081/api/profiles
Authorization: Bearer {SERVICE_TOKEN}
Content-Type: application/json

{
  "userId": "generated-uuid",
  "email": "user@example.com",
  "phoneNumber": "9876543210",
  "firstName": "John",
  ...
}
```

**Step 3:** Customer module validates the service token

**Step 4:** Customer module creates Customer, NameComponents, and Identification records

**Step 5:** Customer module returns success response

**Step 6:** Login module completes registration and returns tokens to frontend

**Error Handling:**
- If customer profile creation fails, the user in login module is NOT deleted
- This is handled to prevent orphaned users
- Retry mechanism can recreate profile later

---

## Security Best Practices Implemented

### What Makes This System Secure?

1. **No Tokens in localStorage** - Prevents XSS attacks
2. **JWT Denylist** - Immediate token revocation on logout
3. **BCrypt Password Hashing** - Industry-standard one-way encryption
4. **RS256 Token Signing** - RSA private/public key cryptography
5. **10-Minute Session Lockout** - Prevents session hijacking
6. **Rate Limiting** - Prevents brute force and DDoS
7. **CORS Configuration** - Only allows requests from trusted origins
8. **Input Validation** - Both frontend (Zod) and backend (Bean Validation)
9. **SQL Injection Prevention** - JPA/Hibernate parameterized queries
10. **Password Complexity Requirements** - Forces strong passwords
11. **OTP 2FA** - Additional authentication factor (planned)
12. **Token Expiry** - Short-lived access tokens, longer refresh tokens
13. **Audit Trail** - All OTP operations logged in database

---

## API Endpoint Summary

### Login Module Endpoints

**Authentication:**
- `POST /api/auth/login` - User login with email/password
- `POST /api/auth/register` - New user registration
- `POST /api/auth/logout` - Logout and token invalidation
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/validate` - Validate JWT token (for other modules)
- `GET /api/auth/lockout-status/{email}` - Check user lockout status

**OTP (Planned):**
- `POST /api/auth/verify-otp` - Verify OTP code
- `POST /api/auth/resend-otp` - Resend OTP

### Customer Module Endpoints

**Profile Management:**
- `POST /api/profiles` - Create customer profile
- `GET /api/profiles/user/{userId}` - Get profile by user ID
- `GET /api/profiles/email/{email}` - Get profile by email
- `PUT /api/profiles/user/{userId}` - Update profile
- `DELETE /api/profiles/user/{userId}` - Delete profile
- `GET /api/profiles` - Get all profiles (admin)
- `GET /api/profiles/search?name={name}` - Search profiles

---

## Configuration Files

### Login Module application.properties
- Server port: 8080
- Database: MySQL connection to nexabank_auth
- Redis: localhost:6379
- JWT: Secret key, expiration times, issuer
- OTPless: Client ID, Client Secret (planned)
- CORS: Allowed origins

### Customer Module application.properties
- Server port: 8081
- Database: MySQL connection to nexabank_customers
- Auth Service URL: http://localhost:8080
- CORS: Allowed origins

---

## Redis Data Structures

### Session Tracking
**Key:** `session:{jti}`
**Value:** `{userId}`
**TTL:** 24 hours (matches access token expiry)

### Token Denylist
**Key:** `denylist:{jti}`
**Value:** `expired`
**TTL:** Token's remaining lifetime

### User Lockout
**Key:** `lockout:{email}`
**Value:** `locked`
**TTL:** 600 seconds (10 minutes)

### OTP Rate Limiting
**Key:** `otp:ratelimit:{userId}`
**Value:** Request count
**TTL:** 600 seconds (10 minutes)

### OTP Resend Cooldown
**Key:** `otp:resend:{userId}`
**Value:** Request count
**TTL:** 60 seconds

---

## Error Handling

### HTTP Status Codes Used

- **200 OK** - Successful operation
- **201 CREATED** - Resource created (registration)
- **400 BAD REQUEST** - Invalid input data
- **401 UNAUTHORIZED** - Invalid credentials or token
- **403 FORBIDDEN** - Insufficient permissions
- **409 CONFLICT** - Resource already exists (duplicate email)
- **423 LOCKED** - User account locked (session control)
- **500 INTERNAL SERVER ERROR** - Server-side error

### Validation Error Response Format
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Email should be valid",
    "phoneNumber": "Must be exactly 10 digits starting with 6, 7, 8, or 9",
    "password": "Password must contain uppercase, lowercase, number, and special character"
  }
}
```

---

## Future Enhancements

1. **OTP 2FA Implementation** - WhatsApp/SMS based verification
2. **Biometric Authentication** - Fingerprint/Face ID support
3. **Device Fingerprinting** - Track and verify known devices
4. **Anomaly Detection** - Detect unusual login patterns
5. **Password Reset via OTP** - Secure password recovery
6. **Email Verification** - Verify email addresses on registration
7. **Account Activity Log** - Track all authentication events
8. **Multi-Factor Authentication Options** - Authenticator apps, hardware keys
9. **Session Management Dashboard** - View and revoke active sessions
10. **Geolocation-Based Security** - Flag logins from unusual locations

---

## Summary

NexaBank implements a robust, secure authentication system with:
- JWT-based stateless authentication
- Bank-style session control (single active session)
- Comprehensive validation at both frontend and backend
- Token denylist for immediate revocation
- Redis-powered session management and rate limiting
- Clean separation between authentication and customer profile data
- Planned OTP-based 2FA for enhanced security
- Industry-standard security practices (BCrypt, RS256, CORS, input validation)

The system is designed to be scalable, maintainable, and secure while providing an excellent user experience.
