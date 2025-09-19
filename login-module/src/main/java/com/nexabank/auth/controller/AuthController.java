package com.nexabank.auth.controller;

import com.nexabank.auth.dto.ApiResponse;
import com.nexabank.auth.dto.AuthResponse;
import com.nexabank.auth.dto.LoginRequest;
import com.nexabank.auth.dto.RegisterRequest;
import com.nexabank.auth.dto.RefreshTokenRequest;
import com.nexabank.auth.entity.User;
import com.nexabank.auth.repository.UserRepository;
import com.nexabank.auth.service.AuthService;
import com.nexabank.auth.util.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            AuthResponse authResponse = authService.registerUser(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User registered successfully!", authResponse));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                                                     HttpServletRequest request) {
        try {
            AuthResponse authResponse = authService.authenticateUser(loginRequest, request);
            return ResponseEntity.ok(ApiResponse.success("Login successful!", authResponse));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            AuthResponse authResponse = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully!", authResponse));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<String>> logoutUser(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String accessToken = authHeader.substring(7);
                
                // Validate token before logout
                if (!jwtUtils.validateJwtToken(accessToken)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error("Invalid token"));
                }
                
                // Perform logout
                authService.logout(accessToken);
                
                // Also invalidate the token in JWT utils
                jwtUtils.invalidateToken(accessToken);
                
                logger.info("User logged out successfully with token ending in: {}", 
                           accessToken.substring(Math.max(0, accessToken.length() - 10)));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("No authorization token provided"));
            }
            return ResponseEntity.ok(ApiResponse.success("User logged out successfully!"));
        } catch (Exception e) {
            logger.error("Logout error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Logout failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/logout-all")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<String>> logoutAllDevices(Authentication authentication) {
        try {
            String userId = ((com.nexabank.auth.service.UserDetailsServiceImpl.UserPrincipal) authentication.getPrincipal()).getId();
            authService.logoutAllDevices(userId);
            return ResponseEntity.ok(ApiResponse.success("Logged out from all devices successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser(Authentication authentication) {
        try {
            com.nexabank.auth.service.UserDetailsServiceImpl.UserPrincipal userPrincipal = 
                (com.nexabank.auth.service.UserDetailsServiceImpl.UserPrincipal) authentication.getPrincipal();
            
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
            userInfo.setUserId(userPrincipal.getId());
            userInfo.setEmail(userPrincipal.getUsername());
            userInfo.setUserType(userPrincipal.getUserType());
            
            return ResponseEntity.ok(ApiResponse.success("User info retrieved successfully!", userInfo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("No Bearer token found"));
            }
            
            String token = authHeader.substring(7);
            
            // Validate JWT token properly
            if (!jwtUtils.validateJwtToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid or expired token"));
            }
            
            // Extract user information from token
            String username = jwtUtils.getUserNameFromJwtToken(token);
            String tokenType = jwtUtils.getTokenType(token);
            
            // Verify user still exists and is active
            User user = userRepository.findByEmail(username)
                    .orElse(null);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User not found"));
            }
            
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User account is not active"));
            }
            
            // Check if token type is correct (should be 'access' for regular requests)
            if (!"access".equals(tokenType)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid token type"));
            }
            
            // Return validation success with user info
            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("valid", true);
            tokenInfo.put("username", username);
            tokenInfo.put("userId", user.getUserId());
            tokenInfo.put("userType", user.getUserType().toString());
            tokenInfo.put("tokenType", tokenType);
            tokenInfo.put("isExpired", jwtUtils.isTokenExpired(token));
            
            return ResponseEntity.ok(ApiResponse.success("Token is valid", tokenInfo));
            
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token has expired"));
        } catch (MalformedJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Malformed token"));
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token validation failed"));
        }
    }
}
