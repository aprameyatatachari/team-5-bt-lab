package com.nexabank.customer.service;

import com.nexabank.customer.dto.AdminStatsResponse;
import com.nexabank.customer.dto.CreateUserRequest;
import com.nexabank.customer.dto.UserDto;
import com.nexabank.customer.dto.BankAccountDto;
import com.nexabank.customer.dto.CustomerDetailsDto;
import com.nexabank.customer.entity.BankAccount;
import com.nexabank.customer.entity.User;
import com.nexabank.customer.repository.BankAccountRepository;
import com.nexabank.customer.repository.TransactionRepository;
import com.nexabank.customer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedAdminService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CustomerDetailsService customerDetailsService;
    private final CustomerUserIntegrationService integrationService;
    private final RestTemplate restTemplate;

    private static final String AUTH_SERVICE_URL = "http://localhost:8080/api";

    public AdminStatsResponse getAdminStats() {
        log.info("Fetching admin stats");
        
        // Get banking stats from local customer service
        int totalAccounts = (int) bankAccountRepository.count();
        double totalDeposits = bankAccountRepository.getTotalDeposits();
        int totalTransactions = (int) transactionRepository.count();
        double totalTransactionVolume = transactionRepository.getTotalTransactionVolume();

        // Get user stats from auth service
        Map<String, Integer> userStats = getUserStatsFromAuthService();
        
        // Get customer stats from new customer service
        List<CustomerDetailsDto> allCustomers = customerDetailsService.getAllCustomers();
        int totalCustomersInNewSystem = allCustomers.size();
        
        // Count active customers in new system
        int activeCustomersInNewSystem = (int) allCustomers.stream()
            .filter(customer -> "ACTIVE".equals(customer.getCustomerStatus()))
            .count();

        return new AdminStatsResponse(
            userStats.getOrDefault("totalUsers", 0),
            Math.max(userStats.getOrDefault("totalCustomers", 0), totalCustomersInNewSystem),
            userStats.getOrDefault("totalAdmins", 0),
            userStats.getOrDefault("totalEmployees", 0),
            Math.max(userStats.getOrDefault("activeUsers", 0), activeCustomersInNewSystem),
            userStats.getOrDefault("lockedUsers", 0),
            totalAccounts,
            totalDeposits,
            totalTransactions,
            totalTransactionVolume
        );
    }

    public List<UserDto> getAllUsers(String userType, String status) {
        log.info("Fetching all users with type: {} and status: {}", userType, status);
        
        try {
            // Get users from auth service
            List<UserDto> authUsers = getUsersFromAuthService(userType, status);
            
            // If requesting customers, also include data from new customer system
            if (userType == null || "CUSTOMER".equalsIgnoreCase(userType)) {
                List<CustomerDetailsDto> customers = customerDetailsService.getAllCustomers();
                
                // Convert customers to UserDto and add to list if not already present
                for (CustomerDetailsDto customer : customers) {
                    if (status == null || status.equalsIgnoreCase(customer.getCustomerStatus())) {
                        UserDto userDto = integrationService.convertCustomerToUser(customer);
                        
                        // Check if user already exists in auth users list
                        boolean exists = authUsers.stream()
                            .anyMatch(authUser -> authUser.getUserId().equals(userDto.getUserId()));
                        
                        if (!exists) {
                            authUsers.add(userDto);
                        }
                    }
                }
            }
            
            return authUsers;
        } catch (Exception e) {
            log.error("Error fetching users: {}", e.getMessage());
            
            // Fallback to customer service only if auth service is down
            if (userType == null || "CUSTOMER".equalsIgnoreCase(userType)) {
                List<CustomerDetailsDto> customers = customerDetailsService.getAllCustomers();
                return customers.stream()
                    .filter(customer -> status == null || status.equalsIgnoreCase(customer.getCustomerStatus()))
                    .map(integrationService::convertCustomerToUser)
                    .collect(Collectors.toList());
            }
            
            throw new RuntimeException("Error fetching users: " + e.getMessage());
        }
    }

    public UserDto getUserById(String userId) {
        log.info("Fetching user by ID: {}", userId);
        
        try {
            // First try to get from auth service
            return getUserFromAuthService(userId);
        } catch (Exception e) {
            log.warn("Failed to get user from auth service, trying customer service: {}", e.getMessage());
            
            // Fallback to customer service
            Optional<CustomerDetailsDto> customerOpt = customerDetailsService.getCustomerByUserId(userId);
            if (customerOpt.isPresent()) {
                return integrationService.convertCustomerToUser(customerOpt.get());
            }
            
            throw new RuntimeException("User not found: " + userId);
        }
    }

    public UserDto createUser(CreateUserRequest request) {
        log.info("Creating user: {}", request.getEmail());
        
        try {
            // Create user in auth service first
            UserDto createdUser = createUserInAuthService(request);
            
            // If it's a customer, also create in customer service
            if ("CUSTOMER".equalsIgnoreCase(request.getUserType())) {
                try {
                    integrationService.createCustomerFromUser(createdUser);
                    log.info("Customer created in new customer system for user: {}", createdUser.getUserId());
                } catch (Exception e) {
                    log.error("Failed to create customer in new system: {}", e.getMessage());
                    // Continue - auth user is created, customer creation can be retried later
                }
            }
            
            return createdUser;
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage());
            throw new RuntimeException("Error creating user: " + e.getMessage());
        }
    }

    public UserDto updateUser(String userId, UserDto userDto) {
        log.info("Updating user: {}", userId);
        
        try {
            // Update in auth service
            UserDto updatedUser = updateUserInAuthService(userId, userDto);
            
            // If it's a customer, also update in customer service
            if ("CUSTOMER".equalsIgnoreCase(userDto.getUserType())) {
                try {
                    integrationService.updateCustomerFromUser(userId, updatedUser);
                    log.info("Customer updated in new customer system for user: {}", userId);
                } catch (Exception e) {
                    log.error("Failed to update customer in new system: {}", e.getMessage());
                    // Continue - auth user is updated
                }
            }
            
            return updatedUser;
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            throw new RuntimeException("Error updating user: " + e.getMessage());
        }
    }

    public UserDto updateUserStatus(String userId, String status) {
        log.info("Updating user status: {} to {}", userId, status);
        
        try {
            // Update in auth service
            UserDto updatedUser = updateUserStatusInAuthService(userId, status);
            
            // Update in customer service if customer exists
            Optional<CustomerDetailsDto> customerOpt = customerDetailsService.getCustomerByUserId(userId);
            if (customerOpt.isPresent()) {
                try {
                    CustomerDetailsDto customer = customerOpt.get();
                    customer.setCustomerStatus(status);
                    customerDetailsService.updateCustomer(customer.getCustomerNumber(), customer);
                    log.info("Customer status updated in new customer system for user: {}", userId);
                } catch (Exception e) {
                    log.error("Failed to update customer status in new system: {}", e.getMessage());
                }
            }
            
            return updatedUser;
        } catch (Exception e) {
            log.error("Error updating user status: {}", e.getMessage());
            throw new RuntimeException("Error updating user status: " + e.getMessage());
        }
    }

    public void deleteUser(String userId) {
        log.info("Deleting user: {}", userId);
        
        try {
            // Delete from customer service first (if exists)
            try {
                integrationService.deleteCustomerByUserId(userId);
                log.info("Customer deleted from new customer system for user: {}", userId);
            } catch (Exception e) {
                log.warn("No customer found in new system for user: {}", userId);
            }
            
            // Delete from auth service
            deleteUserFromAuthService(userId);
            
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage());
            throw new RuntimeException("Error deleting user: " + e.getMessage());
        }
    }

    // Account management methods (unchanged)
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

    // Private helper methods for auth service communication
    private List<UserDto> getUsersFromAuthService(String userType, String status) {
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
    }

    private UserDto getUserFromAuthService(String userId) {
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
        throw new RuntimeException("User not found in auth service");
    }

    private UserDto createUserInAuthService(CreateUserRequest request) {
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
        throw new RuntimeException("Failed to create user in auth service");
    }

    private UserDto updateUserInAuthService(String userId, UserDto userDto) {
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
        throw new RuntimeException("Failed to update user in auth service");
    }

    private UserDto updateUserStatusInAuthService(String userId, String status) {
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
        throw new RuntimeException("Failed to update user status in auth service");
    }

    private void deleteUserFromAuthService(String userId) {
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<String>> response = restTemplate.exchange(
            AUTH_SERVICE_URL + "/admin/users/" + userId,
            HttpMethod.DELETE,
            entity,
            new ParameterizedTypeReference<ApiResponse<String>>() {}
        );

        if (response.getBody() == null || !response.getBody().isSuccess()) {
            throw new RuntimeException("Failed to delete user from auth service");
        }
    }

    private BankAccountDto convertToAccountDto(BankAccount account) {
        // Get user information - try customer service first, then user repository
        String userName = "Unknown User";
        String userEmail = "";
        
        // Try to get from customer service
        Optional<CustomerDetailsDto> customerOpt = customerDetailsService.getCustomerByUserId(account.getUserId());
        if (customerOpt.isPresent()) {
            CustomerDetailsDto customer = customerOpt.get();
            userName = customer.getCustomerFullName();
            userEmail = customer.getCustomerEmailId();
        } else {
            // Fallback to user repository
            Optional<User> userOpt = userRepository.findById(account.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                userName = user.getFirstName() + " " + user.getLastName();
                userEmail = user.getEmail();
            }
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
            log.warn("Auth service not available, returning empty stats: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // Inner class for API response structure
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

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
