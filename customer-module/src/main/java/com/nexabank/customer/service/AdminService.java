package com.nexabank.customer.service;

import com.nexabank.customer.dto.AdminStatsResponse;
import com.nexabank.customer.dto.CreateUserRequest;
import com.nexabank.customer.dto.UserDto;
import com.nexabank.customer.dto.BankAccountDto;
import com.nexabank.customer.entity.BankAccount;
import com.nexabank.customer.entity.Transaction;
import com.nexabank.customer.entity.User;
import com.nexabank.customer.repository.BankAccountRepository;
import com.nexabank.customer.repository.TransactionRepository;
import com.nexabank.customer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String AUTH_SERVICE_URL = "http://localhost:8080/api";

    public AdminStatsResponse getAdminStats() {
        // Get banking stats from local customer service
        int totalAccounts = (int) bankAccountRepository.count();
        double totalDeposits = bankAccountRepository.getTotalDeposits();
        int totalTransactions = (int) transactionRepository.count();
        double totalTransactionVolume = transactionRepository.getTotalTransactionVolume();

        // Get user stats from auth service
        Map<String, Integer> userStats = getUserStatsFromAuthService();

        return new AdminStatsResponse(
            userStats.getOrDefault("totalUsers", 0),
            userStats.getOrDefault("totalCustomers", 0),
            userStats.getOrDefault("totalAdmins", 0),
            userStats.getOrDefault("totalEmployees", 0),
            userStats.getOrDefault("activeUsers", 0),
            userStats.getOrDefault("lockedUsers", 0),
            totalAccounts,
            totalDeposits,
            totalTransactions,
            totalTransactionVolume
        );
    }

    public List<UserDto> getAllUsers(String userType, String status) {
        try {
            StringBuilder url = new StringBuilder(AUTH_SERVICE_URL + "/admin/users");
            boolean hasParams = false;
            
            if (userType != null && !userType.isEmpty()) {
                url.append("?userType=").append(userType);
                hasParams = true;
            }
            
            if (status != null && !status.isEmpty()) {
                url.append(hasParams ? "&" : "?").append("status=").append(status);
            }

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<List<UserDto>>> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ApiResponse<List<UserDto>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            throw new RuntimeException("Failed to fetch users from auth service");
        } catch (Exception e) {
            throw new RuntimeException("Error fetching users: " + e.getMessage());
        }
    }

    public UserDto getUserById(String userId) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                AUTH_SERVICE_URL + "/admin/users/" + userId,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ApiResponse<UserDto>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            throw new RuntimeException("User not found");
        } catch (Exception e) {
            throw new RuntimeException("Error fetching user: " + e.getMessage());
        }
    }

    public UserDto createUser(CreateUserRequest request) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                AUTH_SERVICE_URL + "/admin/users",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ApiResponse<UserDto>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            throw new RuntimeException("Failed to create user");
        } catch (Exception e) {
            throw new RuntimeException("Error creating user: " + e.getMessage());
        }
    }

    public UserDto updateUser(String userId, UserDto userDto) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<UserDto> entity = new HttpEntity<>(userDto, headers);

            ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                AUTH_SERVICE_URL + "/admin/users/" + userId,
                HttpMethod.PUT,
                entity,
                new ParameterizedTypeReference<ApiResponse<UserDto>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            throw new RuntimeException("Failed to update user");
        } catch (Exception e) {
            throw new RuntimeException("Error updating user: " + e.getMessage());
        }
    }

    public UserDto updateUserStatus(String userId, String status) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                AUTH_SERVICE_URL + "/admin/users/" + userId + "/status?status=" + status,
                HttpMethod.PUT,
                entity,
                new ParameterizedTypeReference<ApiResponse<UserDto>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            throw new RuntimeException("Failed to update user status");
        } catch (Exception e) {
            throw new RuntimeException("Error updating user status: " + e.getMessage());
        }
    }

    public void deleteUser(String userId) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
                AUTH_SERVICE_URL + "/admin/users/" + userId,
                HttpMethod.DELETE,
                entity,
                new ParameterizedTypeReference<ApiResponse<String>>() {}
            );

            if (response.getBody() == null || !response.getBody().isSuccess()) {
                throw new RuntimeException("Failed to delete user");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting user: " + e.getMessage());
        }
    }

    // Account management methods
    public List<BankAccountDto> getAllAccounts(String accountType, String status) {
        try {
            List<BankAccount> accounts;
            
            BankAccount.AccountType typeEnum = null;
            BankAccount.AccountStatus statusEnum = null;
            
            if (accountType != null && !accountType.isEmpty()) {
                try {
                    typeEnum = BankAccount.AccountType.valueOf(accountType);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid account type: " + accountType);
                }
            }
            
            if (status != null && !status.isEmpty()) {
                try {
                    statusEnum = BankAccount.AccountStatus.valueOf(status);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid status: " + status);
                }
            }
            
            if (typeEnum != null && statusEnum != null) {
                accounts = bankAccountRepository.findByAccountTypeAndStatus(typeEnum, statusEnum);
            } else if (typeEnum != null) {
                accounts = bankAccountRepository.findByAccountType(typeEnum);
            } else if (statusEnum != null) {
                accounts = bankAccountRepository.findByStatus(statusEnum);
            } else {
                accounts = bankAccountRepository.findAll();
            }

            return accounts.stream()
                    .map(this::convertToAccountDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error fetching accounts: " + e.getMessage());
        }
    }

    public BankAccountDto getAccountById(String accountId) {
        try {
            Optional<BankAccount> accountOpt = bankAccountRepository.findById(accountId);
            if (accountOpt.isPresent()) {
                return convertToAccountDto(accountOpt.get());
            }
            throw new RuntimeException("Account not found");
        } catch (Exception e) {
            throw new RuntimeException("Error fetching account: " + e.getMessage());
        }
    }

    public BankAccountDto updateAccountStatus(String accountId, String status) {
        try {
            Optional<BankAccount> accountOpt = bankAccountRepository.findById(accountId);
            if (accountOpt.isPresent()) {
                BankAccount account = accountOpt.get();
                account.setStatus(BankAccount.AccountStatus.valueOf(status));
                BankAccount updatedAccount = bankAccountRepository.save(account);
                return convertToAccountDto(updatedAccount);
            }
            throw new RuntimeException("Account not found");
        } catch (Exception e) {
            throw new RuntimeException("Error updating account status: " + e.getMessage());
        }
    }

    private BankAccountDto convertToAccountDto(BankAccount account) {
        // Get user information
        String userName = "Unknown User";
        String userEmail = "";
        
        Optional<User> userOpt = userRepository.findById(account.getUserId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userName = user.getFirstName() + " " + user.getLastName();
            userEmail = user.getEmail();
        }

        return new BankAccountDto(
            account.getAccountId(),
            account.getAccountNumber(),
            account.getAccountType().toString(),
            account.getBalance(),
            account.getUserId(),
            userName,
            userEmail,
            account.getStatus().toString(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }

    private Map<String, Integer> getUserStatsFromAuthService() {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse<Map<String, Integer>>> response = restTemplate.exchange(
                AUTH_SERVICE_URL + "/admin/stats",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<ApiResponse<Map<String, Integer>>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return new HashMap<>();
        } catch (Exception e) {
            // Return empty stats if auth service is not available
            return new HashMap<>();
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Note: In a real implementation, you'd pass the current user's JWT token here
        // For now, we'll assume the auth service has a way to validate admin requests
        return headers;
    }

    // Inner class for API response structure
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        // Constructors, getters, and setters
        public ApiResponse() {}

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}
