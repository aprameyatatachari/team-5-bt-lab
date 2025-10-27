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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Tag(name = "Authentication", description = "Core authentication endpoints for login, registration, logout, and token refresh")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RedisSessionService redisSessionService;

    @Operation(
        summary = "User login with email and password",
        description = """
            Authenticate user with email and password credentials. Returns JWT access and refresh tokens.
            
            **Authentication Flow:**
            1. Checks if account is locked out (after 5 failed attempts)
            2. Validates email and password using BCrypt
            3. Generates JWT access token (24h) and refresh token (7d)
            4. Sets 10-minute session lockout (prevents concurrent sessions)
            5. Creates session in Redis for tracking
            
            **Account Lockout Protection:**
            - **Trigger**: 5 consecutive failed login attempts
            - **Duration**: 10 minutes
            - **Cleared**: Successful login or explicit logout
            - Prevents brute force attacks
            
            **Token Details:**
            - **Access Token**: Used for API authentication (24 hours)
            - **Refresh Token**: Used to obtain new access token (7 days)
            - Both tokens signed with RSA-2048 private key
            - JWT ID (jti) included for denylist tracking
            
            **Session Management:**
            - Session created in Redis with 24-hour TTL
            - User locked for 10 minutes (single session enforcement)
            - Must logout to unlock for new login
            
            **Example Use Cases:**
            
            **Customer Login:**
            - Customer enters email and password
            - System validates credentials
            - Returns tokens for authenticated requests
            - Frontend stores tokens securely
            
            **Failed Login:**
            - Invalid credentials tracked
            - After 5 failures, account locked for 10 minutes
            - Lockout status can be checked via /lockout-status endpoint
            
            **Security Features:**
            - BCrypt password hashing (cost factor 10)
            - Failed attempt tracking in Redis
            - Automatic account lockout
            - Session-based lockout (no concurrent sessions)
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login successful - tokens generated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.nexabank.auth.dto.ApiResponse.class),
                examples = @ExampleObject(
                    name = "Successful Login",
                    summary = "Customer successfully logged in",
                    value = """
                        {
                          "success": true,
                          "message": "Login successful",
                          "data": {
                            "accessToken": "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsImp0aSI6IjEyMzQ1Njc4LTkwYWItY2RlZi0xMjM0LTU2Nzg5MGFiY2RlZiIsInVzZXJJZCI6InVzcl8xMjM0NTYiLCJ1c2VyVHlwZSI6IkNVU1RPTUVSIiwicm9sZXMiOiJVU0VSLENVU1RPTUVSIiwiaWF0IjoxNzI5NTg0MDAwLCJleHAiOjE3Mjk2NzA0MDB9...",
                            "refreshToken": "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsImp0aSI6Ijk4NzY1NDMyLTEwZmUtZGNiYS00MzIxLTA5ODc2NTQzMjFmZSIsInVzZXJJZCI6InVzcl8xMjM0NTYiLCJ1c2VyVHlwZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzI5NTg0MDAwLCJleHAiOjE3MzAxODg4MDB9...",
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
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Authentication failed - invalid credentials",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Credentials",
                    summary = "Wrong email or password",
                    value = """
                        {
                          "success": false,
                          "message": "Invalid email or password",
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "423",
            description = "Account locked - too many failed attempts",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Account Locked",
                    summary = "Account locked due to failed attempts",
                    value = """
                        {
                          "success": false,
                          "message": "Account locked. Please try again in 587 seconds",
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Server error during authentication",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                        {
                          "success": false,
                          "message": "Authentication failed: Internal server error",
                          "timestamp": "2025-10-22T10:30:00Z"
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
                    .body(ApiResponse.error("Account locked. Please try again in " + remainingTime + " seconds"));
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
                
                return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password"));
            }
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Authentication failed: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Register new user account",
        description = """
            Register a new user with email, password, and basic details. Creates dual profiles and returns immediate access tokens.
            
            **Registration Process:**
            1. Validates email uniqueness in database
            2. Validates password strength requirements
            3. Hashes password with BCrypt (cost factor 10)
            4. Creates Auth Profile in Auth Service database
            5. Calls Customer Registration Service to create Full Profile
            6. Links both profiles by userId
            7. Generates JWT tokens for immediate login (no separate login required)
            8. Returns tokens and user data
            
            **Dual Profile Architecture:**
            
            **Auth Profile (This Service):**
            - Email, password hash, user type
            - Authentication credentials
            - Login/session management
            - Stored in PostgreSQL
            
            **Full Profile (Customer Service):**
            - Personal details, KYC information
            - Account preferences, documents
            - Transaction history
            - Stored in Customer Service database
            
            **Password Requirements:**
            - Minimum 8 characters
            - At least one uppercase letter
            - At least one lowercase letter
            - At least one number
            - Special characters recommended
            
            **User Types:**
            - **CUSTOMER**: Regular customer account
            - **ADMIN**: Administrative account (restricted)
            - **EMPLOYEE**: Bank employee account
            - **PARTNER**: Business partner account
            
            **Immediate Access:**
            - No email verification required
            - User can login immediately after registration
            - Access and refresh tokens provided in response
            - User status: PENDING (can be activated later)
            
            **Integration:**
            - Calls Customer Registration Service (port 8080)
            - Creates full customer profile automatically
            - Handles service failures gracefully
            - Rolls back on errors
            
            **Example Use Cases:**
            
            **New Customer Registration:**
            - Customer fills registration form
            - System validates and creates accounts
            - Returns tokens for immediate access
            - Customer can start using services
            
            **Employee Registration:**
            - Admin creates employee account
            - Employee type set to EMPLOYEE
            - Additional benefits applied
            - Full access granted immediately
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Registration successful - user created and tokens generated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.nexabank.auth.dto.ApiResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Customer Registration",
                        summary = "New customer successfully registered",
                        value = """
                            {
                              "success": true,
                              "message": "User registered successfully",
                              "data": {
                                "accessToken": "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJuZXdjdXN0b21lckBleGFtcGxlLmNvbSIsImp0aSI6ImFiY2RlZjEyLTM0NTYtNzg5MC1hYmNkLWVmMTIzNDU2Nzg5MCIsInVzZXJJZCI6InVzcl83ODkwMTIiLCJ1c2VyVHlwZSI6IkNVU1RPTUVSIiwicm9sZXMiOiJVU0VSLENVU1RPTUVSIiwiaWF0IjoxNzI5NTg0MDAwLCJleHAiOjE3Mjk2NzA0MDB9...",
                                "refreshToken": "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJuZXdjdXN0b21lckBleGFtcGxlLmNvbSIsImp0aSI6IjEyMzQ1Njc4LTkwYWItY2RlZi0xMjM0LTU2Nzg5MGFiY2RlZiIsInVzZXJJZCI6InVzcl83ODkwMTIiLCJ1c2VyVHlwZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzI5NTg0MDAwLCJleHAiOjE3MzAxODg4MDB9...",
                                "tokenType": "Bearer",
                                "expiresIn": 86400,
                                "user": {
                                  "userId": "usr_789012",
                                  "username": "janedoe",
                                  "email": "newcustomer@example.com",
                                  "userType": "CUSTOMER",
                                  "roles": ["USER", "CUSTOMER"],
                                  "status": "PENDING"
                                }
                              },
                              "timestamp": "2025-10-22T10:30:00Z"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Employee Registration",
                        summary = "New employee account registered",
                        value = """
                            {
                              "success": true,
                              "message": "User registered successfully",
                              "data": {
                                "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
                                "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
                                "tokenType": "Bearer",
                                "expiresIn": 86400,
                                "user": {
                                  "userId": "usr_345678",
                                  "username": "employee1",
                                  "email": "employee@nexabank.com",
                                  "userType": "EMPLOYEE",
                                  "roles": ["USER", "EMPLOYEE", "SUPPORT"],
                                  "status": "ACTIVE"
                                }
                              },
                              "timestamp": "2025-10-22T10:30:00Z"
                            }
                            """
                    )
                }
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Conflict - user already exists with this email",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "User Already Exists",
                    summary = "Email already registered",
                    value = """
                        {
                          "success": false,
                          "message": "User with this email already exists",
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad request - validation failed",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Validation Error",
                    summary = "Invalid input parameters",
                    value = """
                        {
                          "success": false,
                          "message": "Password must be at least 8 characters",
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Server error during registration",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                        {
                          "success": false,
                          "message": "Registration failed: Customer service unavailable",
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
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
        summary = "Register new admin user (Admin access required)",
        description = """
            Register a new admin user. This endpoint requires admin authentication via JWT token.
            
            **Authorization Required:**
            - Must be authenticated with valid JWT token
            - Token must have ADMIN_FULL_ACCESS or ADMIN_USER_MANAGEMENT role
            - Token userType must be "ADMIN"
            
            **Admin Creation Process:**
            1. Validates requesting admin's JWT token
            2. Checks for ADMIN_USER_MANAGEMENT or ADMIN_FULL_ACCESS role
            3. Creates new admin user with specified roles
            4. Returns JWT tokens for the new admin
            
            **Security:**
            - Only existing admins can create new admins
            - Prevents privilege escalation
            - All admin actions are auditable
            
            **Example Request:**
            ```
            POST /api/auth/register/admin
            Authorization: Bearer <admin-jwt-token>
            Content-Type: application/json
            
            {
              "email": "newadmin@nexabank.com",
              "password": "SecurePass123!",
              "firstName": "John",
              "lastName": "Admin",
              "phoneNumber": "9876543210",
              "dateOfBirth": "1990-01-01"
            }
            ```
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Admin registered successfully",
            content = @Content(mediaType = "application/json")
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing admin token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Requires admin privileges"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Conflict - Admin with email already exists"
        )
    })
    @PostMapping("/register/admin")
    public ResponseEntity<?> registerAdmin(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest request) {
        try {
            // Extract JWT token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(com.nexabank.auth.dto.ApiResponse.error("Missing or invalid authorization header"));
            }
            
            String token = authHeader.substring(7);
            
            // Validate token
            if (!jwtTokenService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(com.nexabank.auth.dto.ApiResponse.error("Invalid or expired token"));
            }
            
            // Check if user is admin with proper roles
            String userType = jwtTokenService.extractUserType(token);
            if (!"ADMIN".equals(userType)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.nexabank.auth.dto.ApiResponse.error("Only admins can register new admins"));
            }
            
            // Check for admin roles
            if (!jwtTokenService.hasAnyRole(token, User.Role.ADMIN_FULL_ACCESS, User.Role.ADMIN_USER_MANAGEMENT)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.nexabank.auth.dto.ApiResponse.error("Insufficient privileges. Requires ADMIN_USER_MANAGEMENT or ADMIN_FULL_ACCESS role"));
            }
            
            // Set user type to ADMIN for the new user
            registerRequest.setUserType("ADMIN");
            
            // Register the admin
            User newAdmin = userService.registerUserWithProfile(registerRequest);
            
            // Create JWT tokens for the new admin
            String accessToken = jwtTokenService.generateAccessTokenForUser(newAdmin);
            String refreshToken = jwtTokenService.generateRefreshTokenForUser(newAdmin);
            
            AuthResponse authResponse = new AuthResponse();
            authResponse.setAccessToken(accessToken);
            authResponse.setRefreshToken(refreshToken);
            authResponse.setTokenType("Bearer");
            authResponse.setExpiresIn(86400L);
            authResponse.setUser(newAdmin);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.nexabank.auth.dto.ApiResponse.success("Admin registered successfully", authResponse));
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(com.nexabank.auth.dto.ApiResponse.error("Admin with this email already exists"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(com.nexabank.auth.dto.ApiResponse.error("Admin registration failed: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Logout user and invalidate tokens",
        description = """
            Logout user by invalidating JWT token and clearing session. Token becomes immediately unusable.
            
            **Logout Process:**
            1. Extracts JWT token from Authorization header
            2. Adds JWT ID (jti) to Redis denylist (immediate invalidation)
            3. Clears user session lockout (allows immediate re-login)
            4. Invalidates session in Redis
            5. Returns success confirmation
            
            **Token Invalidation:**
            - JWT ID added to denylist in Redis
            - Denylist TTL matches token expiration
            - All subsequent requests with this token fail validation
            - Works even if token hasn't expired
            
            **Session Cleanup:**
            - Session removed from Redis
            - User lockout cleared (10-minute restriction removed)
            - Failed login attempts reset
            - User can login again immediately
            
            **Security Benefits:**
            - Immediate token revocation
            - Prevents token reuse after logout
            - Protects against stolen tokens
            - Graceful session termination
            
            **Microservices Impact:**
            - Other microservices will reject denylisted tokens
            - Public key verification still works
            - Denylist needs to be checked during verification
            - Consider implementing denylist sync if needed
            
            **Example Use Cases:**
            
            **Standard Logout:**
            - User clicks logout button
            - Frontend sends token to this endpoint
            - Token immediately invalidated
            - User redirected to login page
            
            **Security Logout:**
            - Suspicious activity detected
            - Admin forces user logout
            - All user sessions terminated
            - User must re-authenticate
            
            **Session Timeout:**
            - Frontend detects token expiration
            - Calls logout to cleanup
            - User prompted to login again
            - Clean session state
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Logout successful - token invalidated",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Successful Logout",
                    summary = "User successfully logged out",
                    value = """
                        {
                          "success": true,
                          "message": "Logged out successfully",
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Server error during logout",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                        {
                          "success": false,
                          "message": "Logout failed: Redis connection error",
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
        @Parameter(
            description = "Bearer token in format: Bearer <token>",
            required = true,
            example = "Bearer eyJhbGciOiJSUzI1NiJ9..."
        )
        @RequestHeader("Authorization") String authHeader
    ) {
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
        summary = "Refresh access token using refresh token",
        description = """
            Exchange refresh token for new access and refresh tokens. Implements token rotation for security.
            
            **Token Refresh Process:**
            1. Validates refresh token signature and expiration
            2. Checks if user account is locked out
            3. Extracts user email from refresh token
            4. Generates new access token (24h validity)
            5. Generates new refresh token (7d validity)
            6. Adds old refresh token to denylist (prevents reuse)
            7. Resets user session lockout (extends 10-minute lock)
            8. Returns new token pair
            
            **Token Rotation:**
            - Old refresh token immediately invalidated
            - New refresh token issued with each request
            - Prevents refresh token theft and replay attacks
            - One-time use refresh tokens
            
            **Security Benefits:**
            - Refresh token rotation (one-time use)
            - Old tokens added to denylist
            - Account lockout check before refresh
            - Stolen token detection
            
            **When to Use:**
            - Access token expired or about to expire
            - Proactive token refresh (before expiration)
            - User still active but token old
            - Maintaining session continuity
            
            **Best Practices:**
            - Refresh token before access token expires
            - Store new tokens securely
            - Invalidate old tokens immediately
            - Implement retry logic with exponential backoff
            
            **Token Lifecycle:**
            1. **Login**: Get initial token pair (access + refresh)
            2. **Use**: Use access token for API calls
            3. **Refresh**: Before expiration, refresh tokens
            4. **Repeat**: Continue refresh cycle
            5. **Logout**: Invalidate all tokens
            
            **Example Use Cases:**
            
            **Proactive Refresh:**
            - Frontend checks token expiration
            - 5 minutes before expiry, calls refresh
            - Gets new tokens without user interaction
            - Seamless user experience
            
            **Expired Token:**
            - API returns 401 Unauthorized
            - Frontend automatically calls refresh
            - Retries original request with new token
            - Transparent to user
            
            **Long Session:**
            - User active for multiple hours
            - Tokens refreshed periodically
            - No re-login required
            - Maintains security with rotation
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Token refresh successful - new tokens generated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.nexabank.auth.dto.ApiResponse.class),
                examples = @ExampleObject(
                    name = "Successful Refresh",
                    summary = "Tokens successfully refreshed",
                    value = """
                        {
                          "success": true,
                          "message": "Token refreshed successfully",
                          "data": {
                            "accessToken": "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsImp0aSI6Im5ldy1qd3QtaWQtMTIzIiwidXNlcklkIjoidXNyXzEyMzQ1NiIsInVzZXJUeXBlIjoiQ1VTVE9NRVIiLCJyb2xlcyI6IlVTRVIsQ1VTVE9NRVIiLCJpYXQiOjE3Mjk1ODQwMDAsImV4cCI6MTcyOTY3MDQwMH0...",
                            "refreshToken": "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsImp0aSI6Im5ldy1yZWZyZXNoLWlkLTQ1NiIsInVzZXJJZCI6InVzcl8xMjM0NTYiLCJ1c2VyVHlwZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzI5NTg0MDAwLCJleHAiOjE3MzAxODg4MDB9...",
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
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Token",
                    summary = "Refresh token invalid or expired",
                    value = """
                        {
                          "success": false,
                          "message": "Invalid refresh token",
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "423",
            description = "Account locked - cannot refresh",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Account Locked",
                    value = """
                        {
                          "success": false,
                          "message": "Account locked. Please try again in 450 seconds",
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Server error during token refresh",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                        {
                          "success": false,
                          "message": "Token refresh failed: Internal server error",
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Refresh token request containing the refresh token",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RefreshTokenRequest.class),
                examples = @ExampleObject(
                    name = "Refresh Request",
                    value = """
                        {
                          "refreshToken": "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjdXN0b21lckBleGFtcGxlLmNvbSIsImp0aSI6Im9sZC1yZWZyZXNoLWlkLTc4OSIsInVzZXJJZCI6InVzcl8xMjM0NTYiLCJ1c2VyVHlwZSI6IkNVU1RPTUVSIiwiaWF0IjoxNzI5NTAwMDAwLCJleHAiOjE3MzAxMDQ4MDB9..."
                        }
                        """
                )
            )
        )
        @RequestBody RefreshTokenRequest refreshRequest
    ) {
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
        summary = "Check account lockout status",
        description = """
            Check if a user account is currently locked out and get remaining lockout time.
            
            **Lockout Status Check:**
            - Returns current lockout state for given email
            - Shows remaining time if account is locked
            - Returns 0 seconds if account is not locked
            - Useful for UI to display lockout information
            
            **Account Lockout Scenarios:**
            
            **Failed Login Attempts:**
            - **Trigger**: 5 consecutive failed login attempts
            - **Duration**: 10 minutes (600 seconds)
            - **Purpose**: Prevent brute force attacks
            - **Cleared**: Successful login or manual unlock
            
            **Session Lockout:**
            - **Trigger**: Successful login
            - **Duration**: 10 minutes or until logout
            - **Purpose**: Prevent concurrent sessions
            - **Cleared**: Explicit logout
            
            **Use Cases:**
            
            **Login Page:**
            - Check lockout before showing login form
            - Display countdown timer if locked
            - Show appropriate error message
            - Prevent unnecessary login attempts
            
            **Account Security:**
            - Monitor suspicious login activity
            - Detect brute force attempts
            - Alert user of security events
            - Admin security dashboard
            
            **User Experience:**
            - Show user when they can retry login
            - Prevent frustration with clear messaging
            - Real-time countdown display
            - Automatic re-enable when unlocked
            
            **Integration Examples:**
            
            **Frontend Timer:**
            ```javascript
            // Check lockout status
            const response = await fetch('/api/auth/lockout-status/user@example.com');
            const data = await response.json();
            
            if (data.data.remainingTime > 0) {
              // Show countdown timer
              startCountdown(data.data.remainingTime);
            }
            ```
            
            **Admin Dashboard:**
            - View all locked accounts
            - See remaining lockout times
            - Manually unlock accounts if needed
            - Track security events
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lockout status retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.nexabank.auth.dto.ApiResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Account Locked",
                        summary = "User account is currently locked",
                        value = """
                            {
                              "success": true,
                              "message": "User is locked out",
                              "data": {
                                "remainingTime": 425
                              },
                              "timestamp": "2025-10-22T10:30:00Z"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Account Not Locked",
                        summary = "User account is not locked",
                        value = """
                            {
                              "success": true,
                              "message": "User is not locked out",
                              "timestamp": "2025-10-22T10:30:00Z"
                            }
                            """
                    )
                }
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Server error checking lockout status",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Server Error",
                    value = """
                        {
                          "success": false,
                          "message": "Failed to check lockout status: Redis connection error",
                          "timestamp": "2025-10-22T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/lockout-status/{email}")
    public ResponseEntity<?> getLockoutStatus(
        @Parameter(
            description = "User email address to check lockout status",
            required = true,
            example = "customer@example.com"
        )
        @PathVariable String email
    ) {
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
        summary = "Get JWT public key for token verification",
        description = """
            Retrieve RSA public key in JWK (JSON Web Key) format for independent JWT token verification.
            
            **Purpose:**
            - Enable microservices to verify JWT tokens without calling back to auth service
            - Distribute public key in industry-standard JWK format (RFC 7517)
            - Support stateless authentication architecture
            - Improve performance and reduce auth service load
            
            **JWK Format (RFC 7517):**
            Returns JWKS (JWK Set) with single RSA public key containing:
            - **kty**: Key type (RSA)
            - **alg**: Algorithm (RS256 - RSA with SHA-256)
            - **use**: Key usage (sig - signature verification)
            - **kid**: Key ID (nexabank-auth-key-1)
            - **n**: RSA modulus (Base64URL encoded)
            - **e**: RSA public exponent (Base64URL encoded, typically AQAB)
            
            **RSA-2048 Asymmetric Encryption:**
            - **Private Key** (Auth Service only): Signs JWT tokens
            - **Public Key** (Shared via this endpoint): Verifies token signatures
            - Microservices can verify but **cannot create** tokens
            - More secure than symmetric HMAC (shared secret)
            
            **Microservice Integration:**
            
            **Java (Spring Security):**
            ```java
            @Bean
            public JwtDecoder jwtDecoder() {
                return NimbusJwtDecoder
                    .withJwkSetUri("http://auth-service:3020/api/auth/public-key")
                    .build();
            }
            ```
            
            **Node.js (jwks-rsa + jsonwebtoken):**
            ```javascript
            const jwksClient = require('jwks-rsa');
            const jwt = require('jsonwebtoken');
            
            const client = jwksClient({
              jwksUri: 'http://auth-service:3020/api/auth/public-key',
              cache: true,
              cacheMaxAge: 86400000 // 24 hours
            });
            
            function getKey(header, callback) {
              client.getSigningKey(header.kid, (err, key) => {
                const signingKey = key.publicKey || key.rsaPublicKey;
                callback(null, signingKey);
              });
            }
            
            // Verify token
            jwt.verify(token, getKey, { algorithms: ['RS256'] }, (err, decoded) => {
              if (err) console.error('Invalid token');
              else console.log('Valid token:', decoded);
            });
            ```
            
            **Python (PyJWT):**
            ```python
            import jwt
            from jwt import PyJWKClient
            
            jwks_client = PyJWKClient('http://auth-service:3020/api/auth/public-key')
            signing_key = jwks_client.get_signing_key_from_jwt(token)
            
            data = jwt.decode(
                token,
                signing_key.key,
                algorithms=["RS256"]
            )
            ```
            
            **Best Practices:**
            1. **Cache Public Key**: Fetch once and cache (key doesn't change frequently)
            2. **Periodic Refresh**: Refresh cache every 24 hours or on verification failure
            3. **Handle Rotation**: Support key rotation with kid (key ID) matching
            4. **Denylist Check**: Implement denylist check if needed for immediate revocation
            5. **Algorithm Validation**: Always validate algorithm is RS256
            
            **Security Notes:**
            - Public key can be safely exposed (public endpoint, no auth required)
            - Cannot be used to create new tokens (private key required)
            - Enables zero-trust architecture
            - Supports horizontal scaling of auth service
            
            **Use Cases:**
            
            **Microservice Authentication:**
            - Payment service verifies user tokens
            - Account service validates requests
            - Transaction service checks authorization
            - No round-trip to auth service needed
            
            **API Gateway:**
            - Gateway fetches public key on startup
            - Verifies all incoming requests
            - Routes only valid requests to services
            - Rejects invalid tokens immediately
            
            **Mobile Apps:**
            - App verifies token before API calls
            - Offline token validation
            - Reduced latency
            - Better user experience
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Public key retrieved successfully in JWK format",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "JWK Public Key",
                    summary = "RSA public key in JWKS format",
                    value = """
                        {
                          "keys": [
                            {
                              "kty": "RSA",
                              "alg": "RS256",
                              "use": "sig",
                              "kid": "nexabank-auth-key-1",
                              "n": "xGOG1oTJ5YrKMZKm9FZFvQW8cPJHKqH8vGsJ7rL5TwN8mVxPYvK2hL9sR4mN3pQ6tW8xY9zK7jM5nP8qR2sT4uV6wX0yA1bC3dE5fG7hI9jK1lM3nO5pQ7rS9tU1vW3xY5zA7bC9dE1fG3hI5jK7lM9nO1pQ3rS5tU7vW9xY1zA3bC5dE7fG9hI1jK3lM5nO7pQ9rS1tU3vW5xY7zA9bC1dE3fG5hI7jK9lM1nO3pQ5rS7tU9vW1xY3zA5bC7dE9fG1hI3jK5lM7nO9pQ1rS3tU5vW7xY9zA1bC3dE5fG7hI9jK1lM3nO5pQ7rS9tU1vW3xY5zA7bC9dE1fG3hI5jK7lM9nO1pQ3rS5tU7vW9xY1zA3bC5dE7fG9hI1jK3lM5nO7pQ9rS1tU3vW5xY7zA9bC1dE3fG5hI7jK9lM1nO3pQ5rS7tU9vW1xY3zA5bC7dE9fG1hI3jK5lM7nO9pQ1rS3tU5vW7xY9zA",
                              "e": "AQAB"
                            }
                          ]
                        }
                        """
                )
            )
        )
    })
    @io.swagger.v3.oas.annotations.tags.Tag(name = "Public", description = "Public endpoints accessible without authentication")
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, Object>> getPublicKey() {
        Map<String, Object> response = new HashMap<>();
        
        // Add JWK in keys array (standard JWKS format)
        Map<String, Object> jwk = jwtTokenService.getPublicKeyJWK();
        response.put("keys", java.util.Collections.singletonList(jwk));
        
        return ResponseEntity.ok(response);
    }

}
