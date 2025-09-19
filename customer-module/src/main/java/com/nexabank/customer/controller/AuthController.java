package com.nexabank.customer.controller;

import com.nexabank.customer.dto.ApiResponse;
import com.nexabank.customer.dto.CreateUserRequest;
import com.nexabank.customer.dto.UserDto;
import com.nexabank.customer.service.CustomerAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:4200"})
public class AuthController {

    private final CustomerAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody CreateUserRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());
        
        try {
            UserDto user = authService.registerUser(request);
            return ResponseEntity.ok(ApiResponse.success("User registered successfully", user));
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> loginRequest) {
        log.info("Login request received for email: {}", loginRequest.get("email"));
        
        try {
            String email = loginRequest.get("email");
            String password = loginRequest.get("password");
            
            Map<String, Object> response = authService.loginUser(email, password);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(@RequestHeader("Authorization") String token) {
        log.info("Token validation request received");
        
        try {
            Map<String, Object> response = authService.validateToken(token);
            return ResponseEntity.ok(ApiResponse.success("Token is valid", response));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Token validation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String token) {
        log.info("Logout request received");
        
        try {
            authService.logoutUser(token);
            return ResponseEntity.ok(ApiResponse.success("Logout successful", "User logged out successfully"));
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Logout failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<String>> logoutAllDevices(@RequestHeader("Authorization") String token) {
        log.info("Logout all devices request received");
        
        try {
            authService.logoutUserFromAllDevicesWithToken(token);
            return ResponseEntity.ok(ApiResponse.success("Logout from all devices successful", "User logged out from all devices"));
        } catch (Exception e) {
            log.error("Logout from all devices failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Logout from all devices failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(@RequestBody Map<String, String> refreshRequest) {
        log.info("Token refresh request received");
        
        try {
            String refreshToken = refreshRequest.get("refreshToken");
            Map<String, Object> response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Token refresh failed: " + e.getMessage()));
        }
    }
}
