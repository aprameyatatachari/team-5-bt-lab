package com.nexabank.auth.service;

import com.nexabank.auth.dto.ApiResponse;
import com.nexabank.auth.dto.RegisterRequest;
import com.nexabank.auth.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerIntegrationService {
    
    private final RestTemplate restTemplate;
    private static final String CUSTOMER_SERVICE_URL = "http://localhost:8081/api";
    
    /**
     * Register a new user through the customer service
     */
    public UserDto registerUserInCustomerService(RegisterRequest registerRequest) {
        log.info("Registering user through customer service: {}", registerRequest.getEmail());
        
        try {
            // Create request body as Map to match customer service CreateUserRequest format
            Map<String, Object> customerCreateRequest = new HashMap<>();
            customerCreateRequest.put("email", registerRequest.getEmail());
            customerCreateRequest.put("password", registerRequest.getPassword());
            customerCreateRequest.put("firstName", registerRequest.getFirstName());
            customerCreateRequest.put("lastName", registerRequest.getLastName());
            customerCreateRequest.put("phoneNumber", registerRequest.getPhoneNumber());
            customerCreateRequest.put("userType", registerRequest.getUserType() != null ? 
                registerRequest.getUserType().toString() : "CUSTOMER");
            
            // Handle optional fields
            if (registerRequest.getDateOfBirth() != null) {
                customerCreateRequest.put("dateOfBirth", registerRequest.getDateOfBirth().toString());
            }
            if (registerRequest.getAddress() != null) {
                customerCreateRequest.put("address", registerRequest.getAddress());
            }
            if (registerRequest.getCity() != null) {
                customerCreateRequest.put("city", registerRequest.getCity());
            }
            if (registerRequest.getState() != null) {
                customerCreateRequest.put("state", registerRequest.getState());
            }
            if (registerRequest.getCountry() != null) {
                customerCreateRequest.put("country", registerRequest.getCountry());
            }
            if (registerRequest.getPostalCode() != null) {
                customerCreateRequest.put("postalCode", registerRequest.getPostalCode());
            }
            if (registerRequest.getAadharNumber() != null) {
                customerCreateRequest.put("aadharNumber", registerRequest.getAadharNumber());
            }
            if (registerRequest.getPanNumber() != null) {
                customerCreateRequest.put("panNumber", registerRequest.getPanNumber());
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerCreateRequest, headers);

            ResponseEntity<ApiResponse<UserDto>> response = restTemplate.exchange(
                CUSTOMER_SERVICE_URL + "/admin/users",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ApiResponse<UserDto>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                log.info("User registered successfully in customer service: {}", registerRequest.getEmail());
                return response.getBody().getData();
            } else {
                throw new RuntimeException("Failed to register user in customer service");
            }
        } catch (Exception e) {
            log.error("Error registering user in customer service: {}", e.getMessage());
            throw new RuntimeException("Error registering user in customer service: " + e.getMessage());
        }
    }
    
    /**
     * Check if customer service is available
     */
    public boolean isCustomerServiceAvailable() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                CUSTOMER_SERVICE_URL + "/customers/health",
                HttpMethod.GET,
                entity,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Customer service is not available: {}", e.getMessage());
            return false;
        }
    }
}
