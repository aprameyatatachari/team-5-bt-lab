package com.nexabank.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nexaBankOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:3020");
        devServer.setDescription("Development Server");

        Server prodServer = new Server();
        prodServer.setUrl("https://api.nexabank.com");
        prodServer.setDescription("Production Server");

        Contact contact = new Contact();
        contact.setName("NEXA Bank API Support");
        contact.setUrl("https://www.nexabank.com");
        contact.setEmail("support@nexabank.com");

        License license = new License()
                .name("Proprietary")
                .url("https://www.nexabank.com/license");

        String description = """
                ## Overview
                The Authentication Service API provides comprehensive authentication and authorization services for NEXA Bank's microservices architecture.
                
                ## Key Features
                - **JWT-Based Authentication**: Secure token-based authentication using RSA-2048 asymmetric encryption
                - **User Registration**: Automated user registration with dual-profile creation (Auth + Full Profile)
                - **Session Management**: Redis-backed session tracking with automatic expiration
                - **Token Denylist**: Real-time token invalidation for logout and security events
                - **Account Lockout**: Automatic lockout after failed login attempts with configurable duration
                - **Public Key Distribution**: JWK (JSON Web Key) endpoint for microservices token verification
                - **BCrypt Password Security**: Industry-standard password hashing with salt
                
                ## Authentication Flow
                
                ### Standard Login Flow
                1. **POST /api/auth/login**: Client submits credentials
                2. System validates username/password with BCrypt
                3. JWT access token generated (signed with RSA private key)
                4. Session created in Redis with 24-hour TTL
                5. Client receives access token for subsequent requests
                
                ### Token Verification in Microservices
                1. Microservice fetches public key from **GET /api/auth/public-key** (once, cached)
                2. For each request, verify JWT signature using public key
                3. Check token expiration and claims
                4. No need to call back to auth service for validation
                
                ### Logout Flow
                1. **POST /api/auth/logout**: Client submits token
                2. JWT ID (jti) added to denylist in Redis
                3. Session invalidated
                4. Failed login attempts cleared
                5. Token becomes invalid immediately
                
                ## Security Features
                
                ### RSA-2048 Asymmetric Encryption
                - **Private Key** (Auth Service only): Signs JWT tokens
                - **Public Key** (Shared via /public-key): Verifies token signatures
                - Microservices can verify tokens but **cannot create** them
                - More secure than symmetric HMAC where shared secret allows both signing and verification
                
                ### Token Denylist (Blacklist)
                - Logout immediately invalidates tokens
                - Prevents token reuse after logout
                - Redis-backed with TTL matching token expiration
                - Checked during every token validation
                
                ### Account Lockout Protection
                - **Trigger**: 5 failed login attempts
                - **Duration**: 10 minutes
                - **Cleared**: Successful login or manual unlock
                - Prevents brute force attacks
                
                ### Session Management
                - Redis-backed for high performance
                - Automatic expiration (24 hours default)
                - Session tracking per user
                - Multi-device support
                
                ## JWT Token Structure
                
                ### Access Token Claims
                ```json
                {
                  "sub": "user@example.com",           // Subject (email)
                  "jti": "uuid-v4",                    // JWT ID (for denylist)
                  "userId": "usr_123456",              // User identifier
                  "userType": "CUSTOMER",              // User type
                  "roles": "USER,CUSTOMER",            // Comma-separated roles
                  "iat": 1729584000,                   // Issued at
                  "exp": 1729670400                    // Expiration
                }
                ```
                
                ### Token Expiration
                - **Access Token**: 24 hours (configurable)
                - **Refresh Token**: 7 days (configurable)
                - Tokens can be refreshed using **/api/auth/refresh** endpoint
                
                ## User Registration
                
                ### Dual Profile Creation
                When a user registers:
                1. **Auth Profile** created in Auth Service (authentication credentials)
                2. **Full Profile** created via Customer Registration Service (KYC, personal details)
                3. Both profiles linked by userId
                4. Immediate token generation (no separate login required)
                
                ### Registration Process
                1. Client submits registration details
                2. Validation: email uniqueness, password strength
                3. Password hashed with BCrypt (cost factor 10)
                4. User record created with PENDING status
                5. Call to Customer Registration Service for full profile
                6. JWT token generated and returned
                
                ## Public Key Distribution (JWK)
                
                ### JWK Format (RFC 7517)
                The **/api/auth/public-key** endpoint returns:
                ```json
                {
                  "keys": [{
                    "kty": "RSA",                      // Key type
                    "alg": "RS256",                    // Algorithm
                    "use": "sig",                      // Usage (signature)
                    "kid": "nexabank-auth-key-1",     // Key ID
                    "n": "xGOG1oTJ...",               // Modulus (Base64URL)
                    "e": "AQAB"                        // Exponent (Base64URL)
                  }]
                }
                ```
                
                ### Microservice Integration
                Most JWT libraries support JWK directly:
                
                **Java (Spring Security):**
                ```java
                @Bean
                public JwtDecoder jwtDecoder() {
                    return NimbusJwtDecoder
                        .withJwkSetUri("http://auth-service:3020/api/auth/public-key")
                        .build();
                }
                ```
                
                **Node.js (jsonwebtoken):**
                ```javascript
                const jwksClient = require('jwks-rsa');
                const client = jwksClient({
                  jwksUri: 'http://auth-service:3020/api/auth/public-key'
                });
                ```
                
                ## Error Handling
                
                ### Standard Error Response
                ```json
                {
                  "message": "Error description",
                  "timestamp": "2025-10-22T10:30:00Z"
                }
                ```
                
                ### Common Error Codes
                - **400 Bad Request**: Invalid input parameters
                - **401 Unauthorized**: Authentication failed, invalid credentials
                - **403 Forbidden**: Account locked, insufficient permissions
                - **409 Conflict**: User already exists (registration)
                - **500 Internal Server Error**: Server-side error
                
                ## Rate Limiting & Security
                
                ### Account Lockout
                - **Failed Attempts**: 5
                - **Lockout Duration**: 10 minutes
                - **Reset**: Successful login or manual unlock
                
                ### Redis Performance
                - Session lookup: O(1) complexity
                - Token denylist check: O(1) complexity
                - Automatic TTL expiration
                
                ## Integration Examples
                
                ### Example 1: Standard Login
                ```bash
                curl -X POST http://localhost:3020/api/auth/login \\
                  -H "Content-Type: application/json" \\
                  -d '{
                    "email": "customer@example.com",
                    "password": "SecurePass123!"
                  }'
                ```
                
                **Response:**
                ```json
                {
                  "token": "eyJhbGciOiJSUzI1NiJ9...",
                  "userId": "usr_123456",
                  "userType": "CUSTOMER",
                  "email": "customer@example.com"
                }
                ```
                
                ### Example 2: Register New User
                ```bash
                curl -X POST http://localhost:3020/api/auth/register \\
                  -H "Content-Type: application/json" \\
                  -d '{
                    "username": "johndoe",
                    "email": "john.doe@example.com",
                    "password": "SecurePass123!",
                    "userType": "CUSTOMER"
                  }'
                ```
                
                ### Example 3: Fetch Public Key for Token Verification
                ```bash
                curl -X GET http://localhost:3020/api/auth/public-key
                ```
                
                **Response:**
                ```json
                {
                  "keys": [{
                    "kty": "RSA",
                    "alg": "RS256",
                    "use": "sig",
                    "kid": "nexabank-auth-key-1",
                    "n": "xGOG1oTJ5YrKMZ...",
                    "e": "AQAB"
                  }]
                }
                ```
                
                ### Example 4: Logout
                ```bash
                curl -X POST http://localhost:3020/api/auth/logout \\
                  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
                ```
                
                ## Architecture Notes
                
                ### Microservices Architecture
                - Auth Service runs on port 3020
                - Customer Registration Service: port 8080
                - Product & Pricing Service: port 8080
                - All services use JWT for authentication
                - Public key distributed via JWK endpoint
                
                ### Database & Cache
                - **PostgreSQL**: User credentials, audit logs
                - **Redis**: Sessions, token denylist, lockout tracking
                - Redis TTL matches token expiration for automatic cleanup
                
                ### Scalability
                - Stateless JWT tokens (no server-side session storage for verification)
                - Redis for fast session lookups
                - Horizontal scaling supported
                - Load balancer compatible
                
                ## Best Practices
                
                1. **Token Storage**: Store tokens securely (HttpOnly cookies or secure storage)
                2. **HTTPS Only**: Always use HTTPS in production
                3. **Token Refresh**: Implement token refresh before expiration
                4. **Logout**: Always call logout endpoint to invalidate tokens
                5. **Public Key Caching**: Cache public key in microservices (refresh periodically)
                6. **Error Handling**: Implement proper error handling for 401/403 responses
                
                ## Support
                For API support, integration help, or reporting issues:
                - Email: support@nexabank.com
                - Documentation: https://www.nexabank.com/api-docs
                - Status: https://status.nexabank.com
                """;

        Info info = new Info()
                .title("NEXA Bank - Authentication Service API")
                .version("1.0.0")
                .contact(contact)
                .description(description)
                .termsOfService("https://www.nexabank.com/terms")
                .license(license);

        Tag authTag = new Tag()
                .name("Authentication")
                .description("Core authentication endpoints for login, registration, logout, and token refresh");

        Tag publicTag = new Tag()
                .name("Public")
                .description("Public endpoints for JWT verification - accessible without authentication");

        Tag adminTag = new Tag()
                .name("Admin")
                .description("Administrative endpoints for account management and security operations");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer))
                .tags(List.of(authTag, publicTag, adminTag));
    }
}
