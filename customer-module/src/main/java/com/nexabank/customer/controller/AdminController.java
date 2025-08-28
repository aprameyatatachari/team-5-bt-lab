package com.nexabank.customer.controller;

import com.nexabank.customer.dto.AdminStatsResponse;
import com.nexabank.customer.dto.ApiResponse;
import com.nexabank.customer.dto.UserDto;
import com.nexabank.customer.dto.CreateUserRequest;
import com.nexabank.customer.dto.BankAccountDto;
import com.nexabank.customer.service.EnhancedAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class AdminController {

    @Autowired
    private EnhancedAdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getAdminStats() {
        try {
            AdminStatsResponse stats = adminService.getAdminStats();
            return ResponseEntity.ok(new ApiResponse<>(true, "Admin stats retrieved successfully", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Failed to retrieve admin stats: " + e.getMessage(), null)
            );
        }
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers(
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String status) {
        try {
            List<UserDto> users = adminService.getAllUsers(userType, status);
            return ResponseEntity.ok(new ApiResponse<>(true, "Users retrieved successfully", users));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Failed to retrieve users: " + e.getMessage(), null)
            );
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable String userId) {
        try {
            UserDto user = adminService.getUserById(userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "User retrieved successfully", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Failed to retrieve user: " + e.getMessage(), null)
            );
        }
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserDto user = adminService.createUser(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "User created successfully", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Failed to create user: " + e.getMessage(), null)
            );
        }
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserDto userDto) {
        try {
            UserDto updatedUser = adminService.updateUser(userId, userDto);
            return ResponseEntity.ok(new ApiResponse<>(true, "User updated successfully", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Failed to update user: " + e.getMessage(), null)
            );
        }
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable String userId,
            @RequestParam String status) {
        try {
            UserDto updatedUser = adminService.updateUserStatus(userId, status);
            return ResponseEntity.ok(new ApiResponse<>(true, "User status updated successfully", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Failed to update user status: " + e.getMessage(), null)
            );
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable String userId) {
        try {
            adminService.deleteUser(userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "User deleted successfully", "User deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Failed to delete user: " + e.getMessage(), null)
            );
        }
    }

    // Account management endpoints
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<BankAccountDto>>> getAllAccounts(
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String status) {
        try {
            List<BankAccountDto> accounts = adminService.getAllAccounts(accountType, status);
            return ResponseEntity.ok(new ApiResponse<>(true, "Accounts retrieved successfully", accounts));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Failed to retrieve accounts: " + e.getMessage(), null)
            );
        }
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<ApiResponse<BankAccountDto>> getAccountById(@PathVariable String accountId) {
        try {
            BankAccountDto account = adminService.getAccountById(accountId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Account retrieved successfully", account));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Failed to retrieve account: " + e.getMessage(), null)
            );
        }
    }

    @PutMapping("/accounts/{accountId}/status")
    public ResponseEntity<ApiResponse<BankAccountDto>> updateAccountStatus(
            @PathVariable String accountId,
            @RequestParam String status) {
        try {
            BankAccountDto updatedAccount = adminService.updateAccountStatus(accountId, status);
            return ResponseEntity.ok(new ApiResponse<>(true, "Account status updated successfully", updatedAccount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, "Failed to update account status: " + e.getMessage(), null)
            );
        }
    }
}
