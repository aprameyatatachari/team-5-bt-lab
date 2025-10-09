package com.nexabank.auth.service;

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

    public boolean registerCustomerInCustomerModule(User user, String firstName, String lastName, String phoneNumber) {
        try {
            String url = customerServiceUrl + "/api/customers/register-from-auth";
            
            // Prepare customer data
            Map<String, Object> customerData = new HashMap<>();
            customerData.put("userId", user.getUserId());
            customerData.put("email", user.getEmail());
            customerData.put("firstName", firstName);
            customerData.put("lastName", lastName);
            customerData.put("phoneNumber", phoneNumber);
            customerData.put("userType", user.getUserType().name());
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(customerData, headers);
            
            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            System.err.println("Failed to register customer in customer module: " + e.getMessage());
            return false;
        }
    }

    public boolean createAdminProfile(User user, String firstName, String lastName, String phoneNumber) {
        try {
            String url = customerServiceUrl + "/api/admin/create-profile";
            
            // Prepare admin data
            Map<String, Object> adminData = new HashMap<>();
            adminData.put("userId", user.getUserId());
            adminData.put("email", user.getEmail());
            adminData.put("firstName", firstName);
            adminData.put("lastName", lastName);
            adminData.put("phoneNumber", phoneNumber);
            adminData.put("userType", user.getUserType().name());
            adminData.put("roles", user.getRoles());
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(adminData, headers);
            
            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            System.err.println("Failed to create admin profile in customer module: " + e.getMessage());
            return false;
        }
    }
}