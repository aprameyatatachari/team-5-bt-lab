package com.nexabank.auth.service;

import com.nexabank.auth.dto.RegisterRequest;
import com.nexabank.auth.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class CustomerRegistrationService {

    @Value("${customer.service.url:http://localhost:8081}")
    private String customerServiceUrl;

    private final RestTemplate restTemplate;

    public CustomerRegistrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Register customer profile in customer-module with basic info
     */
    public boolean registerCustomerInCustomerModule(User user, String firstName, String lastName, String phoneNumber) {
        try {
            String url = customerServiceUrl + "/api/profiles";
            
            // Prepare profile data for the new UserProfile entity
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("userId", user.getUserId());
            profileData.put("email", user.getEmail());
            profileData.put("firstName", firstName);
            profileData.put("lastName", lastName);
            profileData.put("phoneNumber", phoneNumber);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(profileData, headers);
            
            // Make the API call to new profile endpoint
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            System.err.println("Failed to create customer profile in customer module: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Register customer profile with full registration data
     */
    public boolean registerCustomerProfileWithFullData(User user, RegisterRequest registerRequest) {
        try {
            String url = customerServiceUrl + "/api/profiles";
            
            // Prepare complete profile data
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("userId", user.getUserId());
            profileData.put("email", user.getEmail());
            profileData.put("firstName", registerRequest.getFirstName());
            profileData.put("lastName", registerRequest.getLastName());
            profileData.put("phoneNumber", registerRequest.getPhoneNumber());
            
            // Add optional fields if provided
            if (registerRequest.getDateOfBirth() != null) {
                profileData.put("dateOfBirth", registerRequest.getDateOfBirth());
            }
            if (registerRequest.getAddress() != null) {
                profileData.put("addressLine1", registerRequest.getAddress());
            }
            if (registerRequest.getCity() != null) {
                profileData.put("city", registerRequest.getCity());
            }
            if (registerRequest.getState() != null) {
                profileData.put("state", registerRequest.getState());
            }
            if (registerRequest.getCountry() != null) {
                profileData.put("country", registerRequest.getCountry());
            }
            if (registerRequest.getPostalCode() != null) {
                profileData.put("postalCode", registerRequest.getPostalCode());
            }
            if (registerRequest.getAadharNumber() != null) {
                profileData.put("aadharNumber", registerRequest.getAadharNumber());
            }
            if (registerRequest.getPanNumber() != null) {
                profileData.put("panNumber", registerRequest.getPanNumber());
            }
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(profileData, headers);
            
            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            System.err.println("Failed to create customer profile with full data: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create admin/employee profile in customer-module
     */
    public boolean createAdminProfile(User user, String firstName, String lastName, String phoneNumber) {
        try {
            String url = customerServiceUrl + "/api/profiles";
            
            // Prepare admin profile data
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("userId", user.getUserId());
            profileData.put("email", user.getEmail());
            profileData.put("firstName", firstName);
            profileData.put("lastName", lastName);
            profileData.put("phoneNumber", phoneNumber);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(profileData, headers);
            
            // Make the API call to the same profile endpoint
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            System.err.println("Failed to create admin profile in customer module: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}