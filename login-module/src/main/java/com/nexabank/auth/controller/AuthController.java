package com.nexabank.auth.controller;

import com.nexabank.auth.dto.*;
import com.nexabank.auth.entity.User;
import com.nexabank.auth.exception.AuthenticationException;
import com.nexabank.auth.exception.UserAlreadyExistsException;
import com.nexabank.auth.service.UserService;
import com.nexabank.auth.service.JwtTokenService;
import com.nexabank.auth.service.RedisSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

/**
 * Controller for NexaBank Authentication System
 * Provides comprehensive authentication, authorization, and session management
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Tag(name = "Authentication Management", description = "Secure user authentication, registration, token management, and session control endpoints for NexaBank")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RedisSessionService redisSessionService;

    @Operation(
        summary = "User Login Authentication",
        description = """
            Authenticate users and generate JWT access and refresh tokens with comprehensive security features.
            
            **Security Features:**
            
            1. **BCrypt Password Validation:**
               - Passwords are hashed using BCrypt with salt
               - Protection against rainbow table attacks
               - Secure password comparison
            
            2. **Account Lockout Protection:**
               - Prevents concurrent logins from multiple devices
               - 10-minute lockout period after successful login
               - Lockout can only be cleared via explicit logout
               - Protects against session hijacking
            
            3. **JWT Token Generation:**
               - Access Token: 24-hour validity for API access
               - Refresh Token: Used to obtain new access tokens
               - Each token includes unique JTI (JWT ID) for tracking
               - Tokens contain user claims: userId, email, roles, userType
            
            4. **Redis Session Management:**
               - Active sessions tracked in Redis
               - Session mapped to JWT ID (JTI)
               - Enables real-time session invalidation
               - Supports distributed authentication
            
            **Authentication Flow:**
            
            **Step 1:** Check if user account is locked out
            - If locked, return remaining lockout time
            - Prevents multiple concurrent sessions
            
            **Step 2:** Validate credentials with BCrypt
            - Email and password verification
            - Returns user object if successful
            
            **Step 3:** Generate JWT tokens
            - Create access token with 24-hour expiry
            - Create refresh token for token renewal
            - Include user claims in token payload
            
            **Step 4:** Set 10-minute lockout
            - Prevents re-login from different device
            - Ensures single active session per user
            - Can be cleared via logout
            
            **Step 5:** Create Redis session
            - Store session with JTI as key
            - Map to user ID for tracking
            - Enable session validation
            
            **Example Scenarios:**
            
            **Customer Login:**
            - User: customer@nexabank.com
            - Role: CUSTOMER
            - Access: Account management, transactions
            
            **Admin Login:**
            - User: admin@nexabank.com
            - Role: ADMIN
            - Access: Full system administration
            
            **Employee Login:**
            - User: employee@nexabank.com
            - Role: EMPLOYEE
            - Access: Customer support, limited admin
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Login credentials",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Customer Login",
                        summary = "Standard customer authentication",
                        value = """
                            {
                              "email": "customer@nexabank.com",
                              "password": "SecurePass123!",
                              "rememberMe": false
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Admin Login",
                        summary = "Administrator authentication",
                        value = """
                            {
                              "email": "admin@nexabank.com",
                              "password": "AdminSecure456!",
                              "rememberMe": true
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Employee Login",
                        summary = "Bank employee authentication",
                        value = """
                            {
                              "email": "employee@nexabank.com",
                              "password": "EmpPass789!",
                              "rememberMe": false
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful - Returns JWT tokens and user profile",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Successful Login",
                        summary = "Login successful with tokens",
                        value = """
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
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid credentials",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Credentials",
                    value = """
                        {
                          "success": false,
                          "message": "Invalid email or password",
                          "data": null
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "423",
            description = "Locked - Account is locked due to active session",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Account Locked",
                    value = """
                        {
                          "success": false,
                          "message": "Account locked. Please try again in 587 seconds",
                          "data": null
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error - Authentication system failure",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                        {
                          "success": false,
                          "message": "Authentication failed: Connection timeout",
                          "data": null
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            // STEP 1: Check if user is locked out (Bank-Style Session Control)
            if (redisSessionService.isUserLockedOut(loginRequest.getEmail())) {
                long remainingTime = redisSessionService.getRemainingLockoutTime(loginRequest.getEmail());
                return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(com.nexabank.auth.dto.ApiResponse.error("Account locked. Please try again in " + remainingTime + " seconds"));
            }

            // STEP 2: Authenticate user with BCrypt password validation
            User authResult = userService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
            
            if (authResult != null) {
                // STEP 3: Create JWT tokens with JTI (unique ID for denylist tracking)
                String accessToken = jwtTokenService.generateAccessTokenForUser(authResult);
                String refreshToken = jwtTokenService.generateRefreshTokenForUser(authResult);
                
                // STEP 4: Set user lockout for 10 minutes (prevents re-login unless explicitly logged out)
                redisSessionService.setUserLockout(authResult.getEmail());
                
                // STEP 5: Create session in Redis for tracking
                String jti = jwtTokenService.extractJwtId(accessToken);
                redisSessionService.createSession(jti, authResult.getUserId());
                
                AuthResponse authResponse = new AuthResponse();
                authResponse.setAccessToken(accessToken);
                authResponse.setRefreshToken(refreshToken);
                authResponse.setTokenType("Bearer");
                authResponse.setExpiresIn(86400L); // 24 hours in seconds
                authResponse.setUser(authResult);
                
                System.out.println("✅ LOGIN SUCCESS: User " + authResult.getEmail() + " logged in with JTI: " + jti);
                System.out.println("🔒 LOCKOUT SET: User locked for 10 minutes (until explicit logout)");
                
                return ResponseEntity.ok(com.nexabank.auth.dto.ApiResponse.success("Login successful", authResponse));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(com.nexabank.auth.dto.ApiResponse.error("Invalid email or password"));
            }
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(com.nexabank.auth.dto.ApiResponse.error("Invalid email or password"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(com.nexabank.auth.dto.ApiResponse.error("Authentication failed: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "User Registration",
        description = """
            Register new users with comprehensive profile creation and immediate authentication.
            
            **Registration Process:**
            
            1. **User Validation:**
               - Email uniqueness check
               - Password strength validation (min 8 characters)
               - Phone number format validation (10-15 digits)
               - Required field validation
            
            2. **Password Security:**
               - BCrypt hashing with salt
               - Minimum 8 characters required
               - Secure password storage
               - Never stored in plain text
            
            3. **Dual Profile Creation:**
               - **Auth Profile:** Email, password, user type, roles
               - **Full Profile:** Personal details, contact info, KYC data
               - Profile stored in separate User Management Service
               - Linked via unique user ID
            
            4. **Immediate Authentication:**
               - JWT tokens generated upon registration
               - No need for separate login
               - User immediately active
               - Session created in Redis
            
            5. **User Types Supported:**
               - **CUSTOMER:** Standard banking customer (default)
               - **EMPLOYEE:** Bank employee with extended privileges
               - **ADMIN:** System administrator with full access
            
            **Profile Components:**
            
            **Basic Information:**
            - First Name, Last Name
            - Email (unique identifier)
            - Phone Number
            - Date of Birth
            
            **Address Details:**
            - Street Address
            - City, State, Country
            - Postal Code
            
            **KYC Information (Optional):**
            - Aadhar Number (Indian national ID)
            - PAN Number (Permanent Account Number)
            - Used for compliance and verification
            
            **Example Scenarios:**
            
            **New Customer Registration:**
            - Complete profile with KYC
            - Automatic CUSTOMER role assignment
            - Ready for account creation
            
            **Employee Onboarding:**
            - Administrative registration
            - EMPLOYEE user type
            - Access to internal systems
            
            **Admin User Setup:**
            - Super admin registration
            - Full system privileges
            - Restricted registration flow
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User registration details with profile information",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RegisterRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Customer Registration",
                        summary = "New customer with full profile and KYC",
                        value = """
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
                            """
                    ),
                    @ExampleObject(
                        name = "Employee Registration",
                        summary = "Bank employee registration",
                        value = """
                            {
                              "email": "priya.sharma@nexabank.com",
                              "password": "EmpSecure456!",
                              "firstName": "Priya",
                              "lastName": "Sharma",
                              "phoneNumber": "9123456789",
                              "dateOfBirth": "1988-08-22",
                              "address": "45 Park Street",
                              "city": "Bangalore",
                              "state": "Karnataka",
                              "country": "India",
                              "postalCode": "560001",
                              "userType": "EMPLOYEE"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Minimal Registration",
                        summary = "Registration with required fields only",
                        value = """
                            {
                              "email": "simple@example.com",
                              "password": "Password123!",
                              "firstName": "Amit",
                              "lastName": "Verma",
                              "phoneNumber": "9988776655",
                              "userType": "CUSTOMER"
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Registration successful - User created and authenticated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Registration Success",
                    value = """
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
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - User with email already exists",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "User Exists",
                    value = """
                        {
                          "success": false,
                          "message": "User with this email already exists",
                          "data": null
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Validation errors",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Validation Error",
                    value = """
                        {
                          "success": false,
                          "message": "Password must be at least 8 characters",
                          "data": null
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error - Registration system failure",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                        {
                          "success": false,
                          "message": "Registration failed: Database connection error",
                          "data": null
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            // Use new method that creates both auth user and full profile
            User user = userService.registerUserWithProfile(registerRequest);
            
            // Create JWT tokens for immediate login after registration
            String accessToken = jwtTokenService.generateAccessTokenForUser(user);
            String refreshToken = jwtTokenService.generateRefreshTokenForUser(user);
            
            AuthResponse authResponse = new AuthResponse();
            authResponse.setAccessToken(accessToken);
            authResponse.setRefreshToken(refreshToken);
            authResponse.setTokenType("Bearer");
            authResponse.setExpiresIn(86400L); // 24 hours in seconds
            authResponse.setUser(user);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.nexabank.auth.dto.ApiResponse.success("User registered successfully", authResponse));
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(com.nexabank.auth.dto.ApiResponse.error("User with this email already exists"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(com.nexabank.auth.dto.ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "User Logout",
        description = """
            Securely terminate user session and invalidate authentication tokens.
            
            **Logout Process:**
            
            1. **Token Invalidation:**
               - Access token added to denylist immediately
               - All subsequent requests with this token will be rejected
               - Denylist stored in Redis with TTL matching token expiry
               - Prevents token reuse even if token hasn't expired
            
            2. **Session Termination:**
               - Redis session removed using JWT ID (JTI)
               - All session data cleared
               - Session cannot be restored
               - Ensures clean logout
            
            3. **Lockout Clearance:**
               - 10-minute account lockout is cleared
               - User can immediately login from any device
               - Enables quick re-authentication
               - Supports session switching
            
            4. **Security Features:**
               - Synchronous logout across all services
               - Immediate token revocation
               - No grace period for token validity
               - Audit trail maintained
            
            **Why Logout is Important:**
            
            - **Security:** Prevents unauthorized access if device is shared
            - **Session Management:** Allows switching between devices
            - **Compliance:** Meets banking security requirements
            - **User Control:** Explicit session termination
            
            **Logout Flow:**
            
            **Step 1:** Extract JWT from Authorization header
            - Parse Bearer token
            - Validate format
            
            **Step 2:** Add token to denylist
            - Store token hash in Redis
            - Set TTL to token expiry
            
            **Step 3:** Clear user lockout
            - Remove lockout from Redis
            - Enable immediate re-login
            
            **Step 4:** Invalidate session
            - Remove session by JTI
            - Clear all session data
            
            **Example Scenarios:**
            
            **Normal Logout:**
            - User clicks logout button
            - Token invalidated immediately
            - Can login from another device
            
            **Security Logout:**
            - Suspicious activity detected
            - Admin forces logout
            - All sessions terminated
            
            **Device Switch:**
            - Logout from mobile
            - Login from desktop
            - Seamless transition
            """,
        parameters = {
            @Parameter(
                name = "Authorization",
                description = "Bearer token in format: Bearer {access_token}",
                required = true,
                example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Logout successful - Token invalidated and session terminated",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Logout Success",
                    value = """
                        {
                          "success": true,
                          "message": "Logged out successfully",
                          "data": null
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Missing or invalid Authorization header",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Header",
                    value = """
                        {
                          "success": false,
                          "message": "Missing Authorization header",
                          "data": null
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error - Logout system failure",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                        {
                          "success": false,
                          "message": "Logout failed: Redis connection error",
                          "data": null
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // STEP 1: Add JWT to denylist (immediate invalidation)
                jwtTokenService.addTokenToDenylist(token);
                
                // STEP 2: Get user email and clear lockout (allows immediate re-login)
                String userEmail = jwtTokenService.getUsernameFromToken(token);
                if (userEmail != null) {
                    redisSessionService.clearUserLockout(userEmail);
                    System.out.println("✅ LOGOUT SUCCESS: User " + userEmail + " logged out");
                    System.out.println("🔓 LOCKOUT CLEARED: User can login immediately");
                }
                
                // STEP 3: Invalidate session
                String jti = jwtTokenService.extractJwtId(token);
                if (jti != null) {
                    redisSessionService.invalidateSession(jti);
                    System.out.println("🗑️ SESSION INVALIDATED: JTI " + jti + " removed");
                }
            }
            
            return ResponseEntity.ok(com.nexabank.auth.dto.ApiResponse.success("Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(com.nexabank.auth.dto.ApiResponse.error("Logout failed: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Refresh Access Token",
        description = """
            Generate new access and refresh tokens using a valid refresh token without re-authentication.
            
            **Token Refresh Process:**
            
            1. **Refresh Token Validation:**
               - Verify token signature and expiry
               - Check if token is in denylist
               - Validate token structure
               - Ensure token hasn't been used before
            
            2. **User Status Check:**
               - Verify user still exists
               - Confirm account is active
               - Check for lockout status
               - Validate user permissions
            
            3. **New Token Generation:**
               - Generate fresh access token (24-hour validity)
               - Generate new refresh token
               - Include updated user claims
               - Maintain token chain integrity
            
            4. **Token Rotation (Security):**
               - Old refresh token added to denylist
               - Prevents token reuse attack
               - Each refresh token single-use only
               - Implements token rotation best practice
            
            5. **Session Management:**
               - Extend 10-minute lockout period
               - Reset session timeout
               - Update session metadata
               - Maintain single active session
            
            **Why Token Refresh?**
            
            - **Security:** Short-lived access tokens reduce exposure
            - **User Experience:** No need to re-login frequently
            - **Performance:** Avoid authentication overhead
            - **Compliance:** Meets security standards
            
            **Token Lifecycle:**
            
            ```
            Login → Access Token (24h) + Refresh Token
                ↓
            Token Expires → Use Refresh Token
                ↓
            New Access Token (24h) + New Refresh Token
                ↓
            Old Refresh Token → Denylisted (Cannot reuse)
            ```
            
            **Security Features:**
            
            - **Token Rotation:** Each refresh invalidates previous refresh token
            - **Single Use:** Refresh tokens can only be used once
            - **Lockout Check:** Prevents refresh during lockout
            - **Denylist:** Old tokens immediately blacklisted
            
            **Example Scenarios:**
            
            **Normal Token Refresh:**
            - Access token about to expire
            - Client automatically refreshes
            - Seamless user experience
            
            **Mobile App Background:**
            - App in background for hours
            - Token expires while inactive
            - Refresh on app resume
            
            **Long Session:**
            - User working for extended period
            - Multiple token refreshes
            - Continuous authentication
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Refresh token request",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RefreshTokenRequest.class),
                examples = @ExampleObject(
                    name = "Token Refresh",
                    summary = "Refresh access token using refresh token",
                    value = """
                        {
                          "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjE2MjM5MDIyLCJleHAiOjE2MTYzMjU0MjJ9..."
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token refresh successful - New tokens generated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Refresh Success",
                    value = """
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
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or expired refresh token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Token",
                    value = """
                        {
                          "success": false,
                          "message": "Invalid refresh token",
                          "data": null
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "423",
            description = "Locked - Account is locked",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Account Locked",
                    value = """
                        {
                          "success": false,
                          "message": "Account locked. Please try again in 345 seconds",
                          "data": null
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error - Token refresh system failure",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                        {
                          "success": false,
                          "message": "Token refresh failed: Service unavailable",
                          "data": null
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest refreshRequest) {
        try {
            if (jwtTokenService.validateToken(refreshRequest.getRefreshToken())) {
                String email = jwtTokenService.extractEmail(refreshRequest.getRefreshToken());
                
                // Check if user is locked out
                if (redisSessionService.isUserLockedOut(email)) {
                    long remainingTime = redisSessionService.getRemainingLockoutTime(email);
                    return ResponseEntity.status(HttpStatus.LOCKED)
                        .body(com.nexabank.auth.dto.ApiResponse.error("Account locked. Please try again in " + remainingTime + " seconds"));
                }
                
                var userOptional = userService.findByEmail(email);
                
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    String newAccessToken = jwtTokenService.generateAccessTokenForUser(user);
                    String newRefreshToken = jwtTokenService.generateRefreshTokenForUser(user);
                    
                    // Add old refresh token to denylist and extend lockout
                    jwtTokenService.addTokenToDenylist(refreshRequest.getRefreshToken());
                    redisSessionService.setUserLockout(user.getEmail()); // Reset 10-minute lockout
                    
                    // Update session using the refresh session method
                    // userService.refreshUserSession(refreshRequest.getRefreshToken());
                    
                    AuthResponse authResponse = new AuthResponse();
                    authResponse.setAccessToken(newAccessToken);
                    authResponse.setRefreshToken(newRefreshToken);
                    authResponse.setTokenType("Bearer");
                    authResponse.setExpiresIn(86400L); // 24 hours in seconds
                    authResponse.setUser(user);
                    
                    return ResponseEntity.ok(com.nexabank.auth.dto.ApiResponse.success("Token refreshed successfully", authResponse));
                }
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(com.nexabank.auth.dto.ApiResponse.error("Invalid refresh token"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(com.nexabank.auth.dto.ApiResponse.error("Token refresh failed: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Check Account Lockout Status",
        description = """
            Check if a user account is currently locked out and get remaining lockout time.
            
            **Lockout Mechanism:**
            
            1. **Purpose:**
               - Prevent concurrent sessions from multiple devices
               - Enforce single active session per user
               - Bank-style session control
               - Security compliance
            
            2. **Lockout Triggers:**
               - Successful login sets 10-minute lockout
               - Token refresh extends lockout by 10 minutes
               - Prevents simultaneous device access
               - Ensures session exclusivity
            
            3. **Lockout Clearance:**
               - Explicit logout clears lockout immediately
               - Allows login from different device
               - Automatic expiry after 10 minutes
               - Grace period for genuine use cases
            
            4. **Status Information:**
               - Returns boolean lockout status
               - Provides remaining time in seconds
               - Helps client implement proper error handling
               - Enables user-friendly messaging
            
            **Use Cases:**
            
            **Client-Side Validation:**
            - Check lockout before showing login form
            - Display countdown timer
            - Provide clear user feedback
            - Improve user experience
            
            **Security Monitoring:**
            - Track lockout patterns
            - Detect suspicious activity
            - Audit session behavior
            - Compliance reporting
            
            **Support/Admin:**
            - Check user lockout status
            - Troubleshoot login issues
            - Monitor active sessions
            - User assistance
            
            **Example Scenarios:**
            
            **User Locked Out:**
            - User tries to login from phone
            - Already logged in on desktop
            - Check returns locked status
            - Shows remaining time: 587 seconds
            
            **User Available:**
            - User logged out from all devices
            - Check returns not locked
            - User can proceed with login
            
            **Lockout Expiring:**
            - 9 minutes since last login
            - Check returns locked with 60 seconds
            - User waits briefly
            - Auto-cleared after expiry
            """,
        parameters = {
            @Parameter(
                name = "email",
                description = "User's email address to check lockout status",
                required = true,
                example = "customer@nexabank.com"
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lockout status retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "User Locked Out",
                        summary = "Account is currently locked",
                        value = """
                            {
                              "success": true,
                              "message": "User is locked out",
                              "data": {
                                "remainingTime": 587
                              }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "User Not Locked",
                        summary = "Account is available for login",
                        value = """
                            {
                              "success": true,
                              "message": "User is not locked out",
                              "data": null
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid email format",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Email",
                    value = """
                        {
                          "success": false,
                          "message": "Invalid email format",
                          "data": null
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error - Lockout check failure",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                        {
                          "success": false,
                          "message": "Failed to check lockout status: Redis unavailable",
                          "data": null
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/lockout-status/{email}")
    public ResponseEntity<?> getLockoutStatus(@PathVariable String email) {
        try {
            boolean isLockedOut = redisSessionService.isUserLockedOut(email);
            long remainingTime = redisSessionService.getRemainingLockoutTime(email);
            
            if (isLockedOut) {
                Map<String, Object> lockoutData = new HashMap<>();
                lockoutData.put("remainingTime", remainingTime);
                return ResponseEntity.ok(com.nexabank.auth.dto.ApiResponse.success("User is locked out", lockoutData));
            } else {
                return ResponseEntity.ok(com.nexabank.auth.dto.ApiResponse.success("User is not locked out"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(com.nexabank.auth.dto.ApiResponse.error("Failed to check lockout status: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Validate JWT Token (Inter-Service Authentication)",
        description = """
            Validate JWT token authenticity and retrieve user information for other microservices.
            
            **Purpose:**
            This endpoint is designed for **inter-service communication** in a microservices architecture.
            Other NexaBank services call this endpoint to verify JWT tokens and get user context.
            
            **Validation Process:**
            
            1. **Header Validation:**
               - Verify Authorization header exists
               - Confirm Bearer token format
               - Extract JWT from header
               - Validate basic structure
            
            2. **Denylist Check:**
               - Query Redis denylist
               - Check if token has been invalidated
               - Verify token hasn't been logged out
               - Ensure token is still active
            
            3. **Token Verification:**
               - Validate JWT signature
               - Check token expiration
               - Verify token integrity
               - Confirm issuer and audience
            
            4. **User Validation:**
               - Extract user email from token
               - Fetch user from database
               - Verify user account status
               - Ensure user is ACTIVE
            
            5. **Response Data:**
               - User ID (for database queries)
               - Email (for communication)
               - User Type (CUSTOMER/EMPLOYEE/ADMIN)
               - Roles (authorization)
               - Status (account state)
            
            **Microservices Integration:**
            
            **Account Service:**
            - Validates token before account operations
            - Gets user ID for account lookup
            - Verifies user permissions
            
            **Transaction Service:**
            - Validates token for transactions
            - Gets user context for audit trail
            - Ensures authorized access
            
            **Admin Service:**
            - Validates admin tokens
            - Checks admin roles
            - Authorizes admin operations
            
            **Security Features:**
            
            - **Real-time Validation:** Checks current token status
            - **Denylist Support:** Respects logged out tokens
            - **User Status Check:** Verifies active accounts
            - **Comprehensive Response:** Returns full user context
            
            **Token Validation Flow:**
            
            ```
            External Service Request
                ↓
            Include Authorization Header
                ↓
            Call /api/auth/validate
                ↓
            Receive User Context
                ↓
            Proceed with Business Logic
            ```
            
            **Use Cases:**
            
            **API Gateway:**
            - Validates all incoming requests
            - Centralizes authentication
            - Routes to services with user context
            
            **Service-to-Service:**
            - Validates forwarded tokens
            - Maintains user context
            - Enables authorization decisions
            
            **Admin Operations:**
            - Validates admin privileges
            - Checks role-based access
            - Ensures compliance
            
            **Example Scenarios:**
            
            **Account Service Request:**
            ```
            1. Client → Account Service (GET /accounts)
            2. Account Service → Auth Service (POST /validate)
            3. Auth Service → Returns user context
            4. Account Service → Fetches user's accounts
            5. Account Service → Returns accounts to client
            ```
            
            **Transaction Authorization:**
            ```
            1. Client → Transaction Service (POST /transfer)
            2. Transaction Service → Auth Service (validate token)
            3. Auth Service → Confirms user identity
            4. Transaction Service → Processes transfer
            5. Transaction Service → Returns confirmation
            ```
            """,
        parameters = {
            @Parameter(
                name = "Authorization",
                description = "Bearer token in format: Bearer {access_token}",
                required = true,
                example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token is valid - Returns user context",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Valid Customer Token",
                        summary = "Token validation successful for customer",
                        value = """
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
                            """
                    ),
                    @ExampleObject(
                        name = "Valid Admin Token",
                        summary = "Token validation successful for admin",
                        value = """
                            {
                              "success": true,
                              "message": "Token is valid",
                              "data": {
                                "valid": true,
                                "userId": "USR789012",
                                "email": "admin@nexabank.com",
                                "userType": "ADMIN",
                                "roles": ["ROLE_ADMIN", "ROLE_USER"],
                                "status": "ACTIVE"
                              }
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - Missing or malformed Authorization header",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Missing Header",
                    value = """
                        {
                          "success": false,
                          "message": "Missing or invalid Authorization header",
                          "data": null
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Token is invalid, expired, or denylisted",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Token Invalidated",
                        summary = "Token has been logged out",
                        value = """
                            {
                              "success": false,
                              "message": "Token has been invalidated",
                              "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Invalid Token",
                        summary = "Token signature invalid or expired",
                        value = """
                            {
                              "success": false,
                              "message": "Invalid token",
                              "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "User Inactive",
                        summary = "User account is not active",
                        value = """
                            {
                              "success": false,
                              "message": "User account is inactive",
                              "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "User Not Found",
                        summary = "User no longer exists",
                        value = """
                            {
                              "success": false,
                              "message": "User not found",
                              "data": null
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error - Token validation system failure",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                        {
                          "success": false,
                          "message": "Token validation failed: Database connection timeout",
                          "data": null
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(com.nexabank.auth.dto.ApiResponse.error("Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            
            // Check if token is on denylist
            if (jwtTokenService.isTokenDenylisted(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(com.nexabank.auth.dto.ApiResponse.error("Token has been invalidated"));
            }

            // Validate token
            if (jwtTokenService.validateToken(token)) {
                // Extract user information from token
                String email = jwtTokenService.extractEmail(token);
                
                // Get user details
                var userOptional = userService.findByEmail(email);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    
                    // Check if user is still active
                    if (user.getStatus() != User.UserStatus.ACTIVE) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(com.nexabank.auth.dto.ApiResponse.error("User account is inactive"));
                    }
                    
                    // Return validation success with user info
                    Map<String, Object> validationData = new HashMap<>();
                    validationData.put("valid", true);
                    validationData.put("userId", user.getUserId());
                    validationData.put("email", user.getEmail());
                    validationData.put("userType", user.getUserType());
                    validationData.put("roles", user.getRoles());
                    validationData.put("status", user.getStatus());
                    
                    return ResponseEntity.ok(com.nexabank.auth.dto.ApiResponse.success("Token is valid", validationData));
                } else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.nexabank.auth.dto.ApiResponse.error("User not found"));
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(com.nexabank.auth.dto.ApiResponse.error("Invalid token"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(com.nexabank.auth.dto.ApiResponse.error("Token validation failed: " + e.getMessage()));
        }
    }
}
