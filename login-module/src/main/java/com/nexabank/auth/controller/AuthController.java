package com.nexabank.auth.controller;

import com.nexabank.auth.dto.*;
import com.nexabank.auth.entity.User;
import com.nexabank.auth.exception.AuthenticationException;
import com.nexabank.auth.exception.UserAlreadyExistsException;
import com.nexabank.auth.service.UserService;
import com.nexabank.auth.service.JwtTokenService;
import com.nexabank.auth.service.RedisSessionService;
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
            // Check if user is locked out
            if (redisSessionService.isUserLockedOut(loginRequest.getEmail())) {
                long remainingTime = redisSessionService.getRemainingLockoutTime(loginRequest.getEmail());
                return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(ApiResponse.error("Account locked. Please try again in " + remainingTime + " seconds"));
            }

            // Authenticate user with BCrypt password validation
            User authResult = userService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
            
            if (authResult != null) {
                // Create JWT tokens with role information
                String accessToken = jwtTokenService.generateAccessTokenForUser(authResult);
                String refreshToken = jwtTokenService.generateRefreshTokenForUser(authResult);
                
                AuthResponse authResponse = new AuthResponse();
                authResponse.setAccessToken(accessToken);
                authResponse.setRefreshToken(refreshToken);
                authResponse.setTokenType("Bearer");
                authResponse.setExpiresIn(86400L); // 24 hours in seconds
                authResponse.setUser(authResult);
                
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
            User user = userService.registerUser(
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getFirstName(),
                registerRequest.getLastName(),
                registerRequest.getPhoneNumber(),
                "CUSTOMER" // Default user type
            );
            
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
                .body(ApiResponse.success("User registered successfully", authResponse));
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("User with this email already exists"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // Get user email from token and clear lockout on explicit logout
                String userEmail = jwtTokenService.getUsernameFromToken(token);
                redisSessionService.clearUserLockout(userEmail);
                
                // Blacklist the token using Redis session service
                jwtTokenService.blacklistToken(token);
                
                // Optional: Call legacy logout method if needed
                // userService.logoutUser(token);
            }
            
            return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Logout failed: " + e.getMessage()));
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
                        .body(ApiResponse.error("Account locked. Please try again in " + remainingTime + " seconds"));
                }
                
                var userOptional = userService.findByEmail(email);
                
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    String newAccessToken = jwtTokenService.generateAccessTokenForUser(user);
                    String newRefreshToken = jwtTokenService.generateRefreshTokenForUser(user);
                    
                    // Blacklist old refresh token and extend lockout
                    jwtTokenService.blacklistToken(refreshRequest.getRefreshToken());
                    redisSessionService.setUserLockout(user.getEmail()); // Reset 10-minute lockout
                    
                    // Update session using the refresh session method
                    // userService.refreshUserSession(refreshRequest.getRefreshToken());
                    
                    AuthResponse authResponse = new AuthResponse();
                    authResponse.setAccessToken(newAccessToken);
                    authResponse.setRefreshToken(newRefreshToken);
                    authResponse.setTokenType("Bearer");
                    authResponse.setExpiresIn(86400L); // 24 hours in seconds
                    authResponse.setUser(user);
                    
                    return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
                }
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid refresh token"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Token refresh failed: " + e.getMessage()));
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
                return ResponseEntity.ok(ApiResponse.success("User is locked out", lockoutData));
            } else {
                return ResponseEntity.ok(ApiResponse.success("User is not locked out"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to check lockout status: " + e.getMessage()));
        }
    }
}