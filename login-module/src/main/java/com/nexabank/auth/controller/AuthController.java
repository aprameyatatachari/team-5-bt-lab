package com.nexabank.auth.controller;

import com.nexabank.auth.dto.*;
import com.nexabank.auth.entity.User;
import com.nexabank.auth.exception.AuthenticationException;
import com.nexabank.auth.exception.UserAlreadyExistsException;
import com.nexabank.auth.service.UserService;
import com.nexabank.auth.service.JwtTokenService;
import com.nexabank.auth.service.RedisSessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Tag(name = "Authentication Management", description = "User authentication, registration, and session management operations")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RedisSessionService redisSessionService;

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

    /**
     * JWT Token Validation Endpoint for Other Modules
     * Other services call this endpoint to verify if a JWT token is valid
     */
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
                String userId = jwtTokenService.getUsernameFromToken(token); // This should return userId
                
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
