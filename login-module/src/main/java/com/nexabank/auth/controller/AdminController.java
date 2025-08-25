package com.nexabank.auth.controller;

import com.nexabank.auth.dto.ApiResponse;
import com.nexabank.auth.dto.CreateUserRequest;
import com.nexabank.auth.dto.UserDto;
import com.nexabank.auth.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers(
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String status) {
        try {
            List<UserDto> users = adminService.getAllUsers(userType, status);
            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve users: " + e.getMessage()));
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable String userId) {
        try {
            UserDto user = adminService.getUserById(userId);
            return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve user: " + e.getMessage()));
        }
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserDto user = adminService.createUser(request);
            return ResponseEntity.ok(ApiResponse.success("User created successfully", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create user: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserDto userDto) {
        try {
            UserDto updatedUser = adminService.updateUser(userId, userDto);
            return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update user: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable String userId,
            @RequestParam String status) {
        try {
            UserDto updatedUser = adminService.updateUserStatus(userId, status);
            return ResponseEntity.ok(ApiResponse.success("User status updated successfully", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update user status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable String userId) {
        try {
            adminService.deleteUser(userId);
            return ResponseEntity.ok(ApiResponse.success("", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getAdminStats() {
        try {
            Map<String, Integer> stats = adminService.getAdminStats();
            return ResponseEntity.ok(ApiResponse.success("Admin statistics retrieved successfully", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve admin statistics: " + e.getMessage()));
        }
    }
}
