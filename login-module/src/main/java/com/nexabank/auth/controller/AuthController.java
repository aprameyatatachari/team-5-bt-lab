package com.nexabank.auth.controller;

import com.nexabank.auth.dto.ApiResponse;
import com.nexabank.auth.dto.AuthResponse;
import com.nexabank.auth.dto.LoginRequest;
import com.nexabank.auth.dto.RegisterRequest;
import com.nexabank.auth.dto.RefreshTokenRequest;
import com.nexabank.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
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
                authService.logout(accessToken);
            }
            return ResponseEntity.ok(ApiResponse.success("User logged out successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
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
    public ResponseEntity<ApiResponse<String>> validateToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(ApiResponse.success("Token is valid!"));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No valid token found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid token"));
        }
    }
}
