package com.nexabank.auth.controller;

import com.nexabank.auth.dto.*;
import com.nexabank.auth.entity.User;
import com.nexabank.auth.exception.AuthenticationException;
import com.nexabank.auth.service.UserService;
import com.nexabank.auth.service.JwtTokenService;
import com.nexabank.auth.service.RedisSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import jakarta.validation.Valid;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Authentication Controller V2
 * Supports login with customerNumber instead of email
 */
@RestController
@RequestMapping("/api/auth/v2")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Tag(name = "Authentication V2", description = "V2 - Authentication endpoints supporting login with customerNumber")
public class AuthControllerV2 {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RedisSessionService redisSessionService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${customer.service.url:http://localhost:1005}")
    private String customerServiceUrl;

    @Operation(
        summary = "User login with customerNumber and password (V2)",
        description = """
            Authenticate user with customerNumber and password credentials (V2 API).
            
            **V2 Authentication Flow:**
            1. Receives customerNumber and password from user
            2. Calls Customer Service to get email for the customerNumber
            3. Checks if account is locked out (after 5 failed attempts)
            4. Validates email and password using BCrypt
            5. Generates JWT access token (24h) and refresh token (7d)
            6. Sets 10-minute session lockout (prevents concurrent sessions)
            7. Creates session in Redis for tracking
            
            **Difference from V1:**
            - **V1**: Uses email + password for login
            - **V2**: Uses customerNumber + password, fetches email from Customer Service
            
            **Customer Number Format:**
            - Example: CUST-20251024-000001
            - Generated during customer registration
            - Unique business identifier for customer
            
            **Account Lockout Protection:**
            - **Trigger**: 5 consecutive failed login attempts
            - **Duration**: 10 minutes
            - **Cleared**: Successful login or explicit logout
            
            **Example Request:**
            ```json
            {
              "customerNumber": "CUST-20251024-000001",
              "password": "MySecurePass123!",
              "rememberMe": true
            }
            ```
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
                    summary = "Customer successfully logged in with customerNumber",
                    value = """
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
                    value = """
                        {
                          "success": false,
                          "message": "Invalid customerNumber or password",
                          "timestamp": "2025-10-29T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Customer not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Customer Not Found",
                    value = """
                        {
                          "success": false,
                          "message": "Customer not found with the given customerNumber",
                          "timestamp": "2025-10-29T10:30:00Z"
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
                    value = """
                        {
                          "success": false,
                          "message": "Account locked. Please try again in 587 seconds",
                          "timestamp": "2025-10-29T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<?> loginV2(@Valid @RequestBody LoginRequestV2 loginRequest, HttpServletRequest request) {
        try {
            // STEP 1: Get email from Customer Service using customerNumber
            String url = customerServiceUrl + "/api/profiles/public/customer/" + loginRequest.getCustomerNumber() + "/email";
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> customerResponse;
            try {
                customerResponse = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.getForEntity(url, Map.class);
            } catch (Exception e) {
                System.err.println("❌ Failed to fetch email from Customer Service: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Customer not found with the given customerNumber"));
            }
            
            if (customerResponse.getStatusCode() != HttpStatus.OK || customerResponse.getBody() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Customer not found with the given customerNumber"));
            }
            
            String email = (String) customerResponse.getBody().get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Email not found for customer"));
            }
            
            System.out.println("✅ V2 LOGIN: Fetched email " + email + " for customerNumber " + loginRequest.getCustomerNumber());
            
            // STEP 2: Check if user is locked out (Bank-Style Session Control)
            if (redisSessionService.isUserLockedOut(email)) {
                long remainingTime = redisSessionService.getRemainingLockoutTime(email);
                return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(ApiResponse.error("Account locked. Please try again in " + remainingTime + " seconds"));
            }

            // STEP 3: Authenticate user with BCrypt password validation (using email)
            User authResult = userService.authenticate(email, loginRequest.getPassword());
            
            if (authResult != null) {
                // STEP 4: Create JWT tokens with JTI (unique ID for denylist tracking)
                String accessToken = jwtTokenService.generateAccessTokenForUser(authResult);
                String refreshToken = jwtTokenService.generateRefreshTokenForUser(authResult);
                
                // STEP 5: Set user lockout for 10 minutes (prevents re-login unless explicitly logged out)
                redisSessionService.setUserLockout(authResult.getEmail());
                
                // STEP 6: Create session in Redis for tracking
                String jti = jwtTokenService.extractJwtId(accessToken);
                redisSessionService.createSession(jti, authResult.getUserId());
                
                AuthResponse authResponse = new AuthResponse();
                authResponse.setAccessToken(accessToken);
                authResponse.setRefreshToken(refreshToken);
                authResponse.setTokenType("Bearer");
                authResponse.setExpiresIn(86400L); // 24 hours in seconds
                authResponse.setUser(authResult);
                
                System.out.println("✅ V2 LOGIN SUCCESS: User " + authResult.getEmail() + " logged in with JTI: " + jti);
                System.out.println("🔒 LOCKOUT SET: User locked for 10 minutes (until explicit logout)");
                
                return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid customerNumber or password"));
            }
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid customerNumber or password"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Authentication failed: " + e.getMessage()));
        }
    }
}
