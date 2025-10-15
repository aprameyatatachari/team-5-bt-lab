# NexaBank Authentication Service - API Documentation

## Overview

The NexaBank Authentication Service provides secure, enterprise-grade authentication and authorization capabilities for the NexaBank banking platform. This service implements JWT-based authentication with Redis session management, BCrypt password encryption, and comprehensive security features.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Security Features](#security-features)
3. [API Endpoints](#api-endpoints)
4. [Authentication Flow](#authentication-flow)
5. [Token Management](#token-management)
6. [Error Handling](#error-handling)
7. [Integration Guide](#integration-guide)

---

## Architecture Overview

### Components

```
┌─────────────────────────────────────────────────────┐
│             NexaBank Authentication Service         │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐    ┌──────────────┐             │
│  │   Auth       │    │   JWT Token  │             │
│  │  Controller  │◄──►│   Service    │             │
│  └──────────────┘    └──────────────┘             │
│         │                    │                     │
│         │                    │                     │
│  ┌──────▼──────┐    ┌───────▼──────┐             │
│  │   User      │    │    Redis     │             │
│  │  Service    │    │   Session    │             │
│  └──────────────┘    └──────────────┘             │
│         │                    │                     │
│  ┌──────▼──────────────────▼──────┐               │
│  │      PostgreSQL Database       │               │
│  │      Redis Cache               │               │
│  └────────────────────────────────┘               │
└─────────────────────────────────────────────────────┘
```

### Technology Stack

- **Framework:** Spring Boot 3.x
- **Security:** Spring Security, JWT (JSON Web Tokens)
- **Password Encryption:** BCrypt
- **Session Store:** Redis
- **Database:** PostgreSQL
- **API Documentation:** OpenAPI 3.0 (Swagger)
- **Validation:** Jakarta Validation

---

## Security Features

### 1. BCrypt Password Encryption

- **Salt Generation:** Unique salt for each password
- **Hash Algorithm:** BCrypt with configurable strength
- **Protection:** Against rainbow table and brute-force attacks
- **Storage:** Only hashed passwords stored in database

### 2. JWT Token Security

- **Access Tokens:** 24-hour validity
- **Refresh Tokens:** Long-lived for token renewal
- **Token Claims:** userId, email, roles, userType, JTI
- **Signature:** HMAC SHA-256 signing algorithm
- **Expiration:** Automatic token expiry
- **Denylist:** Invalidated tokens tracked in Redis

### 3. Session Management

- **Single Session Policy:** One active session per user
- **10-Minute Lockout:** Prevents concurrent device access
- **Redis Storage:** Distributed session tracking
- **JTI Mapping:** Token ID to user ID mapping
- **Automatic Cleanup:** Expired sessions removed

### 4. Account Lockout

- **Duration:** 10 minutes after successful login
- **Purpose:** Prevent session hijacking
- **Clearance:** Explicit logout or automatic expiry
- **Status Check:** Real-time lockout status API

### 5. Token Denylist

- **Purpose:** Immediate token invalidation
- **Storage:** Redis with TTL matching token expiry
- **Logout:** Tokens added to denylist on logout
- **Validation:** All requests checked against denylist

---

## API Endpoints

### Base URL
```
http://localhost:8080/api/auth
```

### Endpoints Summary

| Endpoint | Method | Description | Authentication Required |
|----------|--------|-------------|------------------------|
| `/login` | POST | User authentication | No |
| `/register` | POST | User registration | No |
| `/logout` | POST | User logout | Yes (Bearer Token) |
| `/refresh` | POST | Token refresh | No (Refresh Token) |
| `/lockout-status/{email}` | GET | Check lockout status | No |
| `/validate` | POST | Validate JWT token | Yes (Bearer Token) |

---

## Detailed Endpoint Documentation

### 1. POST /api/auth/login

**Summary:** User Login Authentication

**Description:** Authenticate users and generate JWT access and refresh tokens.

#### Request Body

```json
{
  "email": "customer@nexabank.com",
  "password": "SecurePass123!",
  "rememberMe": false
}
```

#### Success Response (200 OK)

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "userId": "USR123456",
      "email": "customer@nexabank.com",
      "firstName": "John",
      "lastName": "Doe",
      "userType": "CUSTOMER",
      "roles": ["ROLE_USER"],
      "status": "ACTIVE",
      "phoneNumber": "+919876543210"
    }
  }
}
```

#### Error Responses

**401 Unauthorized - Invalid Credentials**
```json
{
  "success": false,
  "message": "Invalid email or password",
  "data": null
}
```

**423 Locked - Account Locked**
```json
{
  "success": false,
  "message": "Account locked. Please try again in 587 seconds",
  "data": null
}
```

#### Authentication Flow

1. **Lockout Check:** Verify user is not locked out
2. **Credential Validation:** BCrypt password verification
3. **Token Generation:** Create access and refresh tokens
4. **Lockout Set:** Activate 10-minute lockout
5. **Session Creation:** Store session in Redis

---

### 2. POST /api/auth/register

**Summary:** User Registration

**Description:** Register new users with comprehensive profile creation.

#### Request Body

```json
{
  "email": "newcustomer@example.com",
  "password": "SecurePass123!",
  "firstName": "Rajesh",
  "lastName": "Kumar",
  "phoneNumber": "9876543210",
  "dateOfBirth": "1990-05-15",
  "address": "123 MG Road",
  "city": "Mumbai",
  "state": "Maharashtra",
  "country": "India",
  "postalCode": "400001",
  "aadharNumber": "123456789012",
  "panNumber": "ABCDE1234F",
  "userType": "CUSTOMER"
}
```

#### Success Response (201 Created)

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "userId": "USR789012",
      "email": "newcustomer@example.com",
      "firstName": "Rajesh",
      "lastName": "Kumar",
      "userType": "CUSTOMER",
      "roles": ["ROLE_USER"],
      "status": "ACTIVE",
      "phoneNumber": "9876543210"
    }
  }
}
```

#### Error Responses

**409 Conflict - User Already Exists**
```json
{
  "success": false,
  "message": "User with this email already exists",
  "data": null
}
```

**400 Bad Request - Validation Error**
```json
{
  "success": false,
  "message": "Password must be at least 8 characters",
  "data": null
}
```

#### Registration Features

- **Dual Profile Creation:** Auth user + Full profile
- **Immediate Authentication:** Tokens generated on registration
- **Password Hashing:** BCrypt encryption
- **Validation:** Email, password, phone format
- **User Types:** CUSTOMER, EMPLOYEE, ADMIN

---

### 3. POST /api/auth/logout

**Summary:** User Logout

**Description:** Terminate user session and invalidate tokens.

#### Request Headers

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Success Response (200 OK)

```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null
}
```

#### Logout Process

1. **Token Denylist:** Add access token to denylist
2. **Lockout Clear:** Remove user lockout
3. **Session Invalidate:** Remove Redis session
4. **Immediate Effect:** Token rejected for all requests

---

### 4. POST /api/auth/refresh

**Summary:** Refresh Access Token

**Description:** Generate new tokens using refresh token.

#### Request Body

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Success Response (200 OK)

```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.NEW_TOKEN...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.NEW_REFRESH...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "userId": "USR123456",
      "email": "customer@nexabank.com",
      "firstName": "John",
      "lastName": "Doe",
      "userType": "CUSTOMER",
      "roles": ["ROLE_USER"],
      "status": "ACTIVE"
    }
  }
}
```

#### Token Rotation

- **Single Use:** Each refresh token used only once
- **Old Token Denylisted:** Previous refresh token invalidated
- **New Tokens:** Both access and refresh tokens regenerated
- **Lockout Extension:** Resets 10-minute lockout

---

### 5. GET /api/auth/lockout-status/{email}

**Summary:** Check Account Lockout Status

**Description:** Check if user account is locked and get remaining time.

#### Path Parameters

- `email` (string, required): User's email address

#### Success Responses

**User Locked Out (200 OK)**
```json
{
  "success": true,
  "message": "User is locked out",
  "data": {
    "remainingTime": 587
  }
}
```

**User Not Locked (200 OK)**
```json
{
  "success": true,
  "message": "User is not locked out",
  "data": null
}
```

#### Use Cases

- **Client-side validation:** Check before login attempt
- **UI feedback:** Display countdown timer
- **Support:** Troubleshoot login issues
- **Monitoring:** Track lockout patterns

---

### 6. POST /api/auth/validate

**Summary:** Validate JWT Token (Inter-Service)

**Description:** Validate token for microservices authentication.

#### Request Headers

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Success Response (200 OK)

```json
{
  "success": true,
  "message": "Token is valid",
  "data": {
    "valid": true,
    "userId": "USR123456",
    "email": "customer@nexabank.com",
    "userType": "CUSTOMER",
    "roles": ["ROLE_USER"],
    "status": "ACTIVE"
  }
}
```

#### Error Responses

**401 Unauthorized - Token Invalidated**
```json
{
  "success": false,
  "message": "Token has been invalidated",
  "data": null
}
```

**401 Unauthorized - User Inactive**
```json
{
  "success": false,
  "message": "User account is inactive",
  "data": null
}
```

#### Validation Checks

1. **Header Format:** Bearer token structure
2. **Denylist Check:** Token not logged out
3. **Signature Verification:** JWT integrity
4. **Expiration Check:** Token not expired
5. **User Status:** Account is ACTIVE

---

## Authentication Flow

### Login Flow Diagram

```
┌─────────┐                           ┌─────────────┐
│ Client  │                           │ Auth Service│
└────┬────┘                           └──────┬──────┘
     │                                       │
     │  POST /login                          │
     │  {email, password}                    │
     ├──────────────────────────────────────►│
     │                                       │
     │                          Check Lockout│
     │                            ┌──────────┤
     │                            │ Redis    │
     │                            └──────────┤
     │                                       │
     │                      Validate Password│
     │                            ┌──────────┤
     │                            │ BCrypt   │
     │                            └──────────┤
     │                                       │
     │                       Generate Tokens │
     │                            ┌──────────┤
     │                            │ JWT      │
     │                            └──────────┤
     │                                       │
     │                          Set Lockout  │
     │                            ┌──────────┤
     │                            │ Redis    │
     │                            └──────────┤
     │                                       │
     │  200 OK                               │
     │  {accessToken, refreshToken, user}    │
     │◄──────────────────────────────────────┤
     │                                       │
```

### Token Refresh Flow

```
┌─────────┐                           ┌─────────────┐
│ Client  │                           │ Auth Service│
└────┬────┘                           └──────┬──────┘
     │                                       │
     │  POST /refresh                        │
     │  {refreshToken}                       │
     ├──────────────────────────────────────►│
     │                                       │
     │                    Validate Refresh   │
     │                            ┌──────────┤
     │                            │ JWT      │
     │                            └──────────┤
     │                                       │
     │                       Check Denylist  │
     │                            ┌──────────┤
     │                            │ Redis    │
     │                            └──────────┤
     │                                       │
     │                       Get User Status │
     │                            ┌──────────┤
     │                            │ Database │
     │                            └──────────┤
     │                                       │
     │                      Generate New     │
     │                            ┌──────────┤
     │                            │ JWT      │
     │                            └──────────┤
     │                                       │
     │                   Denylist Old Token  │
     │                            ┌──────────┤
     │                            │ Redis    │
     │                            └──────────┤
     │                                       │
     │  200 OK                               │
     │  {newAccessToken, newRefreshToken}    │
     │◄──────────────────────────────────────┤
     │                                       │
```

---

## Token Management

### JWT Token Structure

#### Access Token Claims

```json
{
  "sub": "USR123456",
  "email": "customer@nexabank.com",
  "roles": ["ROLE_USER"],
  "userType": "CUSTOMER",
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1634567890,
  "exp": 1634654290
}
```

#### Token Properties

| Property | Description | Value |
|----------|-------------|-------|
| sub | Subject (User ID) | USR123456 |
| email | User email | customer@nexabank.com |
| roles | User roles array | ["ROLE_USER"] |
| userType | User category | CUSTOMER/EMPLOYEE/ADMIN |
| jti | JWT ID (unique) | UUID |
| iat | Issued at timestamp | Unix timestamp |
| exp | Expiration timestamp | Unix timestamp |

### Token Lifecycle

1. **Generation:** On login/registration/refresh
2. **Storage:** Client stores in secure storage
3. **Usage:** Include in Authorization header
4. **Validation:** Checked on every request
5. **Refresh:** Before expiration
6. **Invalidation:** On logout or security event
7. **Expiry:** Automatic after 24 hours

---

## Error Handling

### Standard Error Response

```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

### HTTP Status Codes

| Status Code | Description | Common Causes |
|-------------|-------------|---------------|
| 200 | Success | Request completed successfully |
| 201 | Created | User registered successfully |
| 400 | Bad Request | Validation error, missing fields |
| 401 | Unauthorized | Invalid credentials, expired token |
| 409 | Conflict | Email already exists |
| 423 | Locked | Account locked out |
| 500 | Server Error | Database, Redis, or service failure |

### Common Errors

#### Invalid Credentials
```json
{
  "success": false,
  "message": "Invalid email or password",
  "data": null
}
```

#### Account Locked
```json
{
  "success": false,
  "message": "Account locked. Please try again in 587 seconds",
  "data": null
}
```

#### Token Expired
```json
{
  "success": false,
  "message": "Token has expired",
  "data": null
}
```

#### User Already Exists
```json
{
  "success": false,
  "message": "User with this email already exists",
  "data": null
}
```

---

## Integration Guide

### Frontend Integration

#### 1. Login Implementation

```javascript
async function login(email, password) {
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password, rememberMe: false }),
    });

    const result = await response.json();

    if (result.success) {
      // Store tokens securely
      localStorage.setItem('accessToken', result.data.accessToken);
      localStorage.setItem('refreshToken', result.data.refreshToken);
      localStorage.setItem('user', JSON.stringify(result.data.user));
      
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('Login failed:', error);
    throw error;
  }
}
```

#### 2. Making Authenticated Requests

```javascript
async function makeAuthenticatedRequest(url, options = {}) {
  const accessToken = localStorage.getItem('accessToken');
  
  const response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
  });

  if (response.status === 401) {
    // Token expired, refresh it
    await refreshToken();
    // Retry request with new token
    return makeAuthenticatedRequest(url, options);
  }

  return response.json();
}
```

#### 3. Token Refresh Implementation

```javascript
async function refreshToken() {
  const refreshToken = localStorage.getItem('refreshToken');
  
  const response = await fetch('http://localhost:8080/api/auth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken }),
  });

  const result = await response.json();

  if (result.success) {
    localStorage.setItem('accessToken', result.data.accessToken);
    localStorage.setItem('refreshToken', result.data.refreshToken);
    return result.data;
  } else {
    // Refresh failed, redirect to login
    logout();
    window.location.href = '/login';
  }
}
```

#### 4. Logout Implementation

```javascript
async function logout() {
  const accessToken = localStorage.getItem('accessToken');
  
  try {
    await fetch('http://localhost:8080/api/auth/logout', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
      },
    });
  } finally {
    // Clear tokens regardless of API response
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    window.location.href = '/login';
  }
}
```

### Backend Microservices Integration

#### Validating Tokens in Other Services

```java
@Service
public class AuthenticationService {
    
    @Value("${auth.service.url}")
    private String authServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public UserContext validateToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<ValidationResponse> response = restTemplate.exchange(
                authServiceUrl + "/api/auth/validate",
                HttpMethod.POST,
                entity,
                ValidationResponse.class
            );
            
            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
        } catch (Exception e) {
            throw new UnauthorizedException("Token validation failed");
        }
        
        throw new UnauthorizedException("Invalid token");
    }
}
```

---

## Best Practices

### Security Best Practices

1. **Token Storage:**
   - Use secure storage (not localStorage for sensitive apps)
   - Consider HttpOnly cookies for web apps
   - Clear tokens on logout

2. **Password Requirements:**
   - Minimum 8 characters
   - Mix of uppercase, lowercase, numbers
   - Special characters recommended
   - Regular password rotation

3. **Session Management:**
   - Implement token refresh before expiry
   - Handle lockout scenarios gracefully
   - Clear sessions on security events

4. **Error Handling:**
   - Don't expose sensitive information in errors
   - Log security events for audit
   - Implement rate limiting

### Performance Best Practices

1. **Caching:**
   - Cache user profiles in Redis
   - Cache validation results briefly
   - Use connection pooling

2. **Token Management:**
   - Implement token refresh strategy
   - Clean up expired denylisted tokens
   - Monitor Redis memory usage

3. **Database Queries:**
   - Use indexed queries for user lookup
   - Implement connection pooling
   - Cache frequently accessed data

---

## Support and Contact

For questions, issues, or feature requests regarding the Authentication Service:

- **Documentation:** This file and inline OpenAPI documentation
- **API Explorer:** http://localhost:8080/swagger-ui.html
- **Issues:** Contact development team

---

## Version Information

- **API Version:** 1.0
- **Last Updated:** October 2025
- **Service:** NexaBank Authentication Service
- **Framework:** Spring Boot 3.x

---

## Changelog

### Version 1.0 (October 2025)
- Initial release
- JWT authentication
- Redis session management
- BCrypt password encryption
- Account lockout mechanism
- Token denylist
- Comprehensive API documentation
