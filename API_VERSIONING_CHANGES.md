# API Versioning Changes - Login with CustomerNumber

## Summary

Implemented API versioning for the authentication system to support login with `customerNumber` instead of email.

---

## Changes Made

### 1. Customer Module - Public API

**File**: `customer-module/src/main/java/com/nexabank/customer/controller/UserProfileController.java`

**New Endpoint** (Public - No JWT required):
```
GET /api/profiles/public/customer/{customerNumber}/email
```

**Purpose**: Returns email address for a given customerNumber

**Example Request**:
```bash
curl http://localhost:1005/api/profiles/public/customer/CUST-20251024-000001/email
```

**Example Response**:
```json
{
  "customerNumber": "CUST-20251024-000001",
  "email": "customer@example.com"
}
```

**Security Configuration**: Updated `SecurityConfig.java` to allow public access to `/api/profiles/public/**`

---

### 2. Login Module - API Versioning

#### V1 API (Original - Email-based Login)

**Base Path**: `/api/auth/v1`

**Endpoints**:
- `POST /api/auth/v1/login` - Login with email + password
- `POST /api/auth/v1/register` - Register new user
- `POST /api/auth/v1/logout` - Logout
- `POST /api/auth/v1/refresh` - Refresh token
- `GET /api/auth/v1/lockout-status/{email}` - Check lockout status
- `GET /api/auth/v1/public-key` - Get JWT public key
- `POST /api/auth/v1/register/admin` - Register admin

**Login Request (V1)**:
```json
{
  "email": "customer@example.com",
  "password": "MyPassword123!",
  "rememberMe": true
}
```

---

#### V2 API (New - CustomerNumber-based Login)

**Base Path**: `/api/auth/v2`

**New Endpoint**:
- `POST /api/auth/v2/login` - Login with customerNumber + password

**Login Request (V2)**:
```json
{
  "customerNumber": "CUST-20251024-000001",
  "password": "MyPassword123!",
  "rememberMe": true
}
```

**Login Flow**:
1. User provides customerNumber + password
2. Auth service calls Customer service: `GET /api/profiles/public/customer/{customerNumber}/email`
3. Customer service returns email
4. Auth service validates email + password (same as V1)
5. Returns JWT tokens

**Example Request**:
```bash
curl -X POST http://localhost:3020/api/auth/v2/login \
  -H "Content-Type: application/json" \
  -d '{
    "customerNumber": "CUST-20251024-000001",
    "password": "MyPassword123!",
    "rememberMe": true
  }'
```

**Example Response** (Same as V1):
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "userId": "usr_123456",
      "username": "johndoe",
      "email": "customer@example.com",
      "userType": "CUSTOMER",
      "roles": ["USER", "CUSTOMER"]
    }
  },
  "timestamp": "2025-10-29T10:30:00Z"
}
```

---

#### Legacy API (Backward Compatibility)

**Base Path**: `/api/auth` (unchanged)

All original endpoints still work via delegation to V1:
- `POST /api/auth/login` → delegates to V1
- `POST /api/auth/register` → delegates to V1
- `POST /api/auth/logout` → delegates to V1
- `POST /api/auth/refresh` → delegates to V1
- `GET /api/auth/lockout-status/{email}` → delegates to V1
- `GET /api/auth/public-key` → delegates to V1
- `POST /api/auth/register/admin` → delegates to V1

**Purpose**: Maintains backward compatibility for existing clients

---

## Files Created/Modified

### Customer Module

1. **Modified**: `UserProfileController.java`
   - Added public endpoint: `GET /api/profiles/public/customer/{customerNumber}/email`

2. **Modified**: `SecurityConfig.java`
   - Added public access for `/api/profiles/public/**`

### Login Module

1. **Created**: `LoginRequestV2.java`
   - New DTO for V2 login with customerNumber field

2. **Modified**: `AuthController.java`
   - Changed base path from `/api/auth` to `/api/auth/v1`
   - Updated Swagger tag to "Authentication V1"

3. **Created**: `AuthControllerV2.java`
   - New controller for V2 API
   - Base path: `/api/auth/v2`
   - Implements customerNumber-based login

4. **Created**: `AuthControllerLegacy.java`
   - Maintains backward compatibility
   - Base path: `/api/auth` (original)
   - Delegates all calls to V1

---

## Configuration

### Customer Service URL

The V2 login controller uses the following configuration:

```properties
# application.properties
customer.service.url=http://localhost:1005
```

Default: `http://localhost:1005`

---

## Testing

### Test V1 Login (Email-based):

```bash
# V1 - Email based login
curl -X POST http://localhost:3020/api/auth/v1/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "password123",
    "rememberMe": true
  }'
```

### Test V2 Login (CustomerNumber-based):

```bash
# V2 - CustomerNumber based login
curl -X POST http://localhost:3020/api/auth/v2/login \
  -H "Content-Type: application/json" \
  -d '{
    "customerNumber": "CUST-20251024-000001",
    "password": "password123",
    "rememberMe": true
  }'
```

### Test Legacy Login (Backward Compatibility):

```bash
# Legacy - Still works, delegates to V1
curl -X POST http://localhost:3020/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "password123",
    "rememberMe": true
  }'
```

### Test Public Email Lookup:

```bash
# Public endpoint to get email by customerNumber
curl http://localhost:1005/api/profiles/public/customer/CUST-20251024-000001/email
```

---

## API Version Comparison

| Feature | V1 | V2 | Legacy |
|---------|----|----|--------|
| Base Path | `/api/auth/v1` | `/api/auth/v2` | `/api/auth` |
| Login Identifier | Email | CustomerNumber | Email |
| Password | Required | Required | Required |
| Customer Service Call | No | Yes | No |
| Response Format | JWT Tokens | JWT Tokens | JWT Tokens |
| Backward Compatible | N/A | N/A | Yes (→V1) |

---

## Migration Guide

### For Frontend Applications:

**Option 1: Continue using V1 (Email-based)**
```javascript
// No changes needed
const response = await fetch('/api/auth/v1/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'customer@example.com',
    password: 'password123'
  })
});
```

**Option 2: Migrate to V2 (CustomerNumber-based)**
```javascript
// Update login form to accept customerNumber instead of email
const response = await fetch('/api/auth/v2/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    customerNumber: 'CUST-20251024-000001',
    password: 'password123'
  })
});
```

**Option 3: Use Legacy (No changes)**
```javascript
// Existing code continues to work
const response = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'customer@example.com',
    password: 'password123'
  })
});
```

---

## Benefits

1. **Flexibility**: Users can login with either email (V1) or customerNumber (V2)
2. **Backward Compatibility**: Existing integrations continue to work
3. **Clear Versioning**: Easy to identify which API version is being used
4. **Microservices Integration**: V2 demonstrates inter-service communication
5. **Future-Proof**: Easy to add V3, V4, etc. in the future

---

## Swagger Documentation

All endpoints are documented in Swagger UI:

- V1 Endpoints: http://localhost:3020/swagger-ui.html (tag: "Authentication V1")
- V2 Endpoints: http://localhost:3020/swagger-ui.html (tag: "Authentication V2")
- Customer Public API: http://localhost:1005/swagger-ui.html

---

## Security Considerations

1. **Public Endpoint**: `/api/profiles/public/customer/{customerNumber}/email` is intentionally public
   - Only returns customerNumber and email (no sensitive data)
   - Required for V2 login flow
   - No PII beyond email is exposed

2. **Rate Limiting**: Consider adding rate limiting to public endpoints to prevent abuse

3. **Logging**: V2 login logs include both customerNumber and email for audit trail

---

**Date**: October 29, 2025
**Status**: Production Ready ✅
