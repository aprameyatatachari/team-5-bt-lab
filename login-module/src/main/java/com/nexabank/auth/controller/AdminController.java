package com.nexabank.auth.controller;

import com.nexabank.auth.dto.ApiResponse;
import com.nexabank.auth.entity.User;
import com.nexabank.auth.service.JwtTokenService;
import com.nexabank.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}, allowCredentials = "true")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Missing or invalid authorization header"));
            }

            String token = authHeader.substring(7);
            
            // Check if user has admin privileges
            if (!jwtTokenService.isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Insufficient privileges. Admin access required."));
            }

            // Get all users (in a real system, you'd paginate this)
            List<User> users = userService.getAllUsers();
            
            // Remove sensitive information before returning
            List<Map<String, Object>> sanitizedUsers = users.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("userId", user.getUserId());
                userMap.put("email", user.getEmail());
                userMap.put("userType", user.getUserType());
                userMap.put("status", user.getStatus());
                userMap.put("roles", user.getRoles());
                userMap.put("lastLogin", user.getLastLogin());
                userMap.put("failedLoginAttempts", user.getFailedLoginAttempts());
                userMap.put("createdAt", user.getCreatedAt());
                return userMap;
            }).toList();

            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", sanitizedUsers));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve users: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<?> updateUserRoles(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Missing or invalid authorization header"));
            }

            String token = authHeader.substring(7);
            
            // Check if user has admin privileges
            if (!jwtTokenService.hasRole(token, User.Role.ADMIN_USER_MANAGEMENT)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Insufficient privileges. User management access required."));
            }

            String action = (String) request.get("action"); // "add" or "remove"
            String roleName = (String) request.get("role");
            
            if (action == null || roleName == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Action and role are required"));
            }

            User.Role role;
            try {
                role = User.Role.valueOf(roleName);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid role: " + roleName));
            }

            if ("add".equals(action)) {
                userService.addRoleToUser(userId, role);
            } else if ("remove".equals(action)) {
                userService.removeRoleFromUser(userId, role);
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid action. Use 'add' or 'remove'"));
            }

            return ResponseEntity.ok(ApiResponse.success("User role updated successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to update user role: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Missing or invalid authorization header"));
            }

            String token = authHeader.substring(7);
            
            // Check if user has admin privileges
            if (!jwtTokenService.hasRole(token, User.Role.ADMIN_USER_MANAGEMENT)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Insufficient privileges. User management access required."));
            }

            String statusName = (String) request.get("status");
            
            if (statusName == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Status is required"));
            }

            User.UserStatus status;
            try {
                status = User.UserStatus.valueOf(statusName);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status: " + statusName));
            }

            boolean updated = userService.updateUserStatus(userId, status);
            
            if (updated) {
                return ResponseEntity.ok(ApiResponse.success("User status updated successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to update user status: " + e.getMessage()));
        }
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Missing or invalid authorization header"));
            }

            String token = authHeader.substring(7);
            
            // Check if user has admin privileges
            if (!jwtTokenService.isAdmin(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Insufficient privileges. Admin access required."));
            }

            Map<String, Object> stats = userService.getUserStatistics();
            
            return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved successfully", stats));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve dashboard stats: " + e.getMessage()));
        }
    }
}