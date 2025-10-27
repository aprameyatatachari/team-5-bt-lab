# Security Implementation Summary

## Overview
This document describes the JWT-based security implementation across the NexaBank microservices architecture.

## Components

### 1. Login Module Security

#### Admin Registration Endpoint
**Endpoint**: `POST /api/auth/register/admin`

**Purpose**: Allow existing admins to create new admin accounts

**Security**:
- Requires JWT authentication (Authorization: Bearer <token>)
- Validates admin token using JwtTokenService
- Checks for required roles:
  - `ADMIN_FULL_ACCESS` (can create any admin)
  - `ADMIN_USER_MANAGEMENT` (can create new admins)

**Response Codes**:
- `201 Created`: Admin successfully created with JWT tokens
- `400 Bad Request`: Invalid registration data or user already exists
- `401 Unauthorized`: Missing or invalid JWT token
- `403 Forbidden`: Valid token but insufficient privileges

#### SecurityConfig Updates
- Added `.requestMatchers("/api/auth/register/admin").authenticated()`
- Protects admin registration from public access
- All other auth endpoints remain public (login, customer registration, etc.)

---

### 2. Customer Module Security

#### JWT Authentication Filter
**File**: `JwtAuthenticationFilter.java`

**Responsibilities**:
1. Extracts Bearer token from Authorization header
2. Validates token using JwtValidationService
3. Sets SecurityContext with authentication details
4. Adds request attributes for downstream use:
   - `userId` - The authenticated user's ID
   - `userType` - Either "ADMIN" or "CUSTOMER"
   - `roles` - List of user roles

**Flow**:
```
Request → Extract Token → Validate → Set SecurityContext → Continue
```

#### JWT Validation Service
**File**: `JwtValidationService.java`

**Features**:
- Fetches RSA public key from auth service on startup
- Validates JWT signatures using JJWT library
- Extracts claims: userId, userType, roles, email
- Helper methods:
  - `hasRole(token, role)` - Check specific role
  - `hasAnyRole(token, roles)` - Check multiple roles
  - `isAdmin(token)` - Check if admin user
  - `isCustomer(token)` - Check if customer user

**Configuration**:
- Auth service URL: `${auth.service.url:http://localhost:8080}`
- Public key endpoint: `/api/auth/public-key`

#### SecurityConfig
**Protected Endpoints**:
- GET `/api/profiles/user/{userId}` - Get customer by userId
- PUT `/api/profiles/user/{userId}` - Update customer
- PUT `/api/profiles/user/{userId}/address` - Update address
- PUT `/api/profiles/user/{userId}/name` - Update name
- PUT `/api/profiles/user/{userId}/identification` - Update ID documents
- DELETE `/api/profiles/user/{userId}` - Delete customer
- GET `/api/profiles/customer/{customerNumber}` - Get by business ID
- GET `/api/profiles/email/{email}` - Get by email
- GET `/api/profiles/search` - Search customers
- GET `/api/profiles` - Get all customers

**Public Endpoints**:
- POST `/api/profiles` - Registration (called by auth service)
- Swagger/OpenAPI documentation endpoints

#### Authorization Logic
**File**: `UserProfileController.java`

**Helper Methods**:
```java
private boolean isAuthorized(HttpServletRequest request, String targetUserId)
```
- Checks if user can access target userId's data
- Admins: Can access all data
- Customers: Can only access their own data (userId match)

```java
private boolean isAdmin(HttpServletRequest request)
```
- Checks if requesting user is an admin
- Used for admin-only endpoints (getAll, search, delete)

**Endpoint Authorization**:

| Endpoint | Authorization Rule |
|----------|-------------------|
| GET /user/{userId} | Admin OR own userId |
| PUT /user/{userId} | Admin OR own userId |
| PUT /user/{userId}/address | Admin OR own userId |
| PUT /user/{userId}/name | Admin OR own userId |
| PUT /user/{userId}/identification | Admin OR own userId |
| DELETE /user/{userId} | Admin only |
| GET /search | Admin only |
| GET (all) | Admin only |

---

## JWT Token Structure

### Admin Token Example
```json
{
  "sub": "admin@nexabank.com",
  "userId": "admin123",
  "userType": "ADMIN",
  "roles": [
    "ADMIN_FULL_ACCESS",
    "ADMIN_VIEW",
    "ADMIN_REPORTS",
    "ADMIN_SYSTEM_CONFIG",
    "ADMIN_USER_MANAGEMENT"
  ],
  "jti": "unique-token-id",
  "iat": 1234567890,
  "exp": 1234571490
}
```

### Customer Token Example
```json
{
  "sub": "customer@email.com",
  "userId": "user456",
  "userType": "CUSTOMER",
  "roles": [
    "CUSTOMER_VIEW",
    "CUSTOMER_TRANSACTION"
  ],
  "jti": "unique-token-id",
  "iat": 1234567890,
  "exp": 1234571490
}
```

---

## Security Flow Diagrams

### Admin Creating Another Admin
```
Admin Client
    ↓ POST /api/auth/register/admin (with admin JWT)
AuthController
    ↓ Extract & validate JWT
JwtTokenService
    ↓ Check roles (ADMIN_FULL_ACCESS or ADMIN_USER_MANAGEMENT)
    ↓ Create new admin user
UserService
    ↓ Return new admin with JWT tokens
Admin Client
```

### Customer Accessing Own Profile
```
Customer Client
    ↓ GET /api/profiles/user/{userId} (with JWT)
JwtAuthenticationFilter
    ↓ Extract token, validate, set SecurityContext
UserProfileController
    ↓ isAuthorized(): check userId match
    ↓ userId from token == path userId? → Allow
CustomerService
    ↓ Return profile data
Customer Client
```

### Admin Accessing Any Profile
```
Admin Client
    ↓ GET /api/profiles/user/{userId} (with admin JWT)
JwtAuthenticationFilter
    ↓ Extract token, validate, set SecurityContext
UserProfileController
    ↓ isAuthorized(): check userType
    ↓ userType == "ADMIN"? → Allow
CustomerService
    ↓ Return profile data
Admin Client
```

### Customer Attempting to Access Another's Profile
```
Customer Client
    ↓ GET /api/profiles/user/{otherUserId} (with JWT)
JwtAuthenticationFilter
    ↓ Extract token, validate, set SecurityContext
UserProfileController
    ↓ isAuthorized(): check userId match
    ↓ userId from token != path userId? → Deny
    ↓ Return 403 Forbidden
Customer Client
```

---

## API Usage Examples

### 1. Admin Registering New Admin

**Request**:
```http
POST http://localhost:8080/api/auth/register/admin
Authorization: Bearer <admin-jwt-token>
Content-Type: application/json

{
  "username": "newadmin",
  "email": "newadmin@nexabank.com",
  "password": "SecurePass123!",
  "firstName": "New",
  "lastName": "Admin",
  "phoneNumber": "+1234567890",
  "userType": "ADMIN"
}
```

**Success Response (201)**:
```json
{
  "message": "Admin registered successfully",
  "userId": "admin789",
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 3600000
}
```

**Error Responses**:
- `401`: Missing or invalid JWT token
- `403`: Valid token but user lacks admin privileges
- `400`: Invalid registration data

### 2. Customer Accessing Own Profile

**Request**:
```http
GET http://localhost:1005/api/profiles/user/user456
Authorization: Bearer <customer-jwt-token>
```

**Success Response (200)**:
```json
{
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "customerNumber": "CUST-20251023-000001",
  "userId": "user456",
  "email": "customer@email.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "customerStatus": "ACTIVE",
  "kycStatus": "COMPLETED"
}
```

### 3. Admin Viewing All Customers

**Request**:
```http
GET http://localhost:1005/api/profiles
Authorization: Bearer <admin-jwt-token>
```

**Success Response (200)**:
```json
[
  {
    "customerId": "...",
    "customerNumber": "CUST-20251023-000001",
    "userId": "user456",
    "email": "customer1@email.com",
    "firstName": "John",
    "lastName": "Doe"
  },
  {
    "customerId": "...",
    "customerNumber": "CUST-20251023-000002",
    "userId": "user789",
    "email": "customer2@email.com",
    "firstName": "Jane",
    "lastName": "Smith"
  }
]
```

**Error Response (403)** - Customer trying to access:
```json
"Access denied: Admin privileges required"
```

### 4. Customer Attempting Unauthorized Access

**Request**:
```http
GET http://localhost:1005/api/profiles/user/otherUser123
Authorization: Bearer <customer-jwt-token-for-user456>
```

**Error Response (403)**:
```json
"Access denied: You can only access your own profile"
```

---

## Testing Checklist

### Login Module Tests
- [ ] Public customer registration works without token
- [ ] Admin registration requires JWT token
- [ ] Admin registration rejects invalid/expired tokens
- [ ] Admin registration rejects customer tokens
- [ ] Admin registration rejects admin tokens without proper roles
- [ ] Admin with ADMIN_FULL_ACCESS can create admins
- [ ] Admin with ADMIN_USER_MANAGEMENT can create admins
- [ ] Admin with only ADMIN_VIEW cannot create admins

### Customer Module Tests
- [ ] POST /api/profiles works without JWT (public registration)
- [ ] GET /user/{userId} rejects requests without JWT
- [ ] Customer can GET their own profile
- [ ] Customer cannot GET another customer's profile
- [ ] Admin can GET any customer's profile
- [ ] Customer can UPDATE their own profile
- [ ] Customer cannot UPDATE another customer's profile
- [ ] Admin can UPDATE any customer's profile
- [ ] Only admin can DELETE customer profiles
- [ ] Only admin can GET all profiles
- [ ] Only admin can SEARCH profiles

---

## Configuration

### application.properties (customer-module)

```properties
# Auth Service Configuration
auth.service.url=http://localhost:8080

# Server Configuration
server.port=1005

# JWT Configuration (no secret needed - validates with public key)
```

### Environment Variables (Optional)
```bash
AUTH_SERVICE_URL=http://localhost:8080
```

---

## Error Handling

### Common Error Responses

**401 Unauthorized** - Missing or invalid JWT:
```json
{
  "timestamp": "2025-01-23T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token is missing or invalid",
  "path": "/api/profiles/user/123"
}
```

**403 Forbidden** - Insufficient permissions:
```json
"Access denied: You can only access your own profile"
```
or
```json
"Access denied: Admin privileges required"
```

**404 Not Found** - Resource not found:
```json
"Customer profile not found for userId: xyz"
```

---

## Security Best Practices Implemented

1. **JWT Validation**: All tokens validated using RSA public key from auth service
2. **Stateless Authentication**: No session state stored in customer module
3. **Role-Based Access Control**: Different permissions for ADMIN vs CUSTOMER
4. **User Isolation**: Customers can only access their own data
5. **Admin Oversight**: Admins can manage all customer accounts
6. **Audit Trail**: INSERT-ONLY paradigm preserves all data changes
7. **Secure Registration**: Admin creation requires authenticated admin
8. **Public Key Distribution**: Auth service shares public key for distributed validation

---

## Future Enhancements

1. **Rate Limiting**: Add rate limiting per user/IP
2. **Token Refresh**: Implement automatic token refresh
3. **Role Hierarchy**: More granular admin permissions
4. **Audit Logging**: Log all admin actions on customer data
5. **Session Management**: Track active sessions per user
6. **IP Whitelisting**: Restrict admin actions to specific IPs
7. **2FA for Admins**: Require two-factor auth for admin operations

---

## Troubleshooting

### Issue: "JWT token is missing or invalid"
**Solution**: Ensure Authorization header is present with format: `Bearer <token>`

### Issue: "Access denied: Admin privileges required"
**Solution**: Verify user has admin role in JWT token

### Issue: "Access denied: You can only access your own profile"
**Solution**: Ensure userId in path matches userId in JWT token

### Issue: Public key fetch fails
**Solution**: 
1. Verify auth service is running on http://localhost:8080
2. Check `/api/auth/public-key` endpoint is accessible
3. Review customer-module logs for connection errors

---

## Contact & Support

For questions about security implementation:
- Review this document
- Check code comments in security package
- Examine Swagger documentation at http://localhost:1005/swagger-ui.html
