package com.nexabank.auth.controller;

import com.nexabank.auth.dto.ApiResponse;
import com.nexabank.auth.dto.CreateUserRequest;
import com.nexabank.auth.dto.UserDto;
import com.nexabank.auth.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminController {

    @Autowired
    private AdminService adminService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String CUSTOMER_SERVICE_URL = "http://localhost:8081/api";

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers(
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String status) {
        try {
            // Redirect to customer service for unified user management
            StringBuilder url = new StringBuilder(CUSTOMER_SERVICE_URL + "/admin/users");
            boolean hasParams = false;
            
            if (userType != null && !userType.isEmpty()) {
                url.append("?userType=").append(userType);
                hasParams = true;
            }
            
            if (status != null && !status.isEmpty()) {
                url.append(hasParams ? "&" : "?").append("status=").append(status);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<List<UserDto>>> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ApiResponse<List<UserDto>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", response.getBody().getData()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to retrieve users from customer service"));
            }
        } catch (Exception e) {
            // Fallback to local auth service if customer service is unavailable
            List<UserDto> users = adminService.getAllUsers(userType, status);
            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully (from auth service)", users));
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable String userId) {
        try {
            // Redirect to customer service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                CUSTOMER_SERVICE_URL + "/admin/users/" + userId,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ApiResponse<UserDto>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response.getBody().getData()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to retrieve user from customer service"));
            }
        } catch (Exception e) {
            // Fallback to local auth service
            try {
                UserDto user = adminService.getUserById(userId);
                return ResponseEntity.ok(ApiResponse.success("User retrieved successfully (from auth service)", user));
            } catch (Exception fallbackException) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to retrieve user: " + fallbackException.getMessage()));
            }
        }
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            // Redirect user creation to customer service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                CUSTOMER_SERVICE_URL + "/admin/users",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ApiResponse<UserDto>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success("User created successfully", response.getBody().getData()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to create user in customer service"));
            }
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
            // Redirect to customer service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UserDto> entity = new HttpEntity<>(userDto, headers);

            ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                CUSTOMER_SERVICE_URL + "/admin/users/" + userId,
                HttpMethod.PUT,
                entity,
                new ParameterizedTypeReference<ApiResponse<UserDto>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success("User updated successfully", response.getBody().getData()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to update user in customer service"));
            }
        } catch (Exception e) {
            // Fallback to local auth service
            try {
                UserDto updatedUser = adminService.updateUser(userId, userDto);
                return ResponseEntity.ok(ApiResponse.success("User updated successfully (in auth service)", updatedUser));
            } catch (Exception fallbackException) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to update user: " + fallbackException.getMessage()));
            }
        }
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable String userId,
            @RequestParam String status) {
        try {
            // Redirect to customer service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                CUSTOMER_SERVICE_URL + "/admin/users/" + userId + "/status?status=" + status,
                HttpMethod.PUT,
                entity,
                new ParameterizedTypeReference<ApiResponse<UserDto>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success("User status updated successfully", response.getBody().getData()));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to update user status in customer service"));
            }
        } catch (Exception e) {
            // Fallback to local auth service
            try {
                UserDto updatedUser = adminService.updateUserStatus(userId, status);
                return ResponseEntity.ok(ApiResponse.success("User status updated successfully (in auth service)", updatedUser));
            } catch (Exception fallbackException) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to update user status: " + fallbackException.getMessage()));
            }
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable String userId) {
        try {
            // Redirect to customer service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                CUSTOMER_SERVICE_URL + "/admin/users/" + userId,
                HttpMethod.DELETE,
                entity,
                new ParameterizedTypeReference<ApiResponse<String>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success("", "User deleted successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to delete user in customer service"));
            }
        } catch (Exception e) {
            // Fallback to local auth service
            try {
                adminService.deleteUser(userId);
                return ResponseEntity.ok(ApiResponse.success("", "User deleted successfully (from auth service)"));
            } catch (Exception fallbackException) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to delete user: " + fallbackException.getMessage()));
            }
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
