package com.nexabank.customer.service;

import com.nexabank.customer.dto.CustomerDetailsDto;
import com.nexabank.customer.dto.CustomerNameComponentDto;
import com.nexabank.customer.dto.UserDto;
import com.nexabank.customer.entity.CustomerDetails;
import com.nexabank.customer.entity.CustomerNameComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerUserIntegrationService {
    
    private final CustomerDetailsService customerDetailsService;
    private final CustomerNameComponentService nameComponentService;
    
    /**
     * Create a new customer with integrated user management
     */
    public CustomerDetailsDto createCustomerFromUser(UserDto userDto) {
        log.info("Creating customer from user: {}", userDto.getEmail());
        
        // Create CustomerDetails from UserDto
        CustomerDetailsDto customerDto = new CustomerDetailsDto();
        customerDto.setCustomerType(CustomerDetails.INDIVIDUAL);
        customerDto.setCustomerFullName(userDto.getFirstName() + " " + userDto.getLastName());
        customerDto.setCustomerEmailId(userDto.getEmail());
        customerDto.setCustomerMobileNumber(userDto.getPhoneNumber());
        customerDto.setCustomerStatus("ACTIVE");
        customerDto.setCustomerCountryOfOrigination("India");
        customerDto.setUserId(userDto.getUserId());
        customerDto.setEffectiveDate(LocalDateTime.now());
        
        // Save customer details
        CustomerDetailsDto savedCustomer = customerDetailsService.createCustomer(customerDto);
        
        // Create name components
        createNameComponentsFromUser(savedCustomer.getCustomerNumber(), userDto);
        
        log.info("Customer created successfully with number: {}", savedCustomer.getCustomerNumber());
        return savedCustomer;
    }
    
    /**
     * Get customer details by user ID
     */
    public Optional<CustomerDetailsDto> getCustomerByUserId(String userId) {
        log.info("Fetching customer by userId: {}", userId);
        return customerDetailsService.getCustomerByUserId(userId);
    }
    
    /**
     * Update customer when user details change
     */
    public CustomerDetailsDto updateCustomerFromUser(String userId, UserDto userDto) {
        log.info("Updating customer from user: {}", userId);
        
        Optional<CustomerDetailsDto> existingCustomerOpt = customerDetailsService.getCustomerByUserId(userId);
        
        if (existingCustomerOpt.isPresent()) {
            CustomerDetailsDto existingCustomer = existingCustomerOpt.get();
            
            // Update customer details
            existingCustomer.setCustomerFullName(userDto.getFirstName() + " " + userDto.getLastName());
            existingCustomer.setCustomerEmailId(userDto.getEmail());
            existingCustomer.setCustomerMobileNumber(userDto.getPhoneNumber());
            
            // Update customer
            CustomerDetailsDto updatedCustomer = customerDetailsService.updateCustomer(
                existingCustomer.getCustomerNumber(), existingCustomer);
            
            // Update name components
            updateNameComponentsFromUser(existingCustomer.getCustomerNumber(), userDto);
            
            log.info("Customer updated successfully: {}", existingCustomer.getCustomerNumber());
            return updatedCustomer;
        } else {
            // Create new customer if doesn't exist
            return createCustomerFromUser(userDto);
        }
    }
    
    /**
     * Convert CustomerDetailsDto to UserDto for backward compatibility
     */
    public UserDto convertCustomerToUser(CustomerDetailsDto customerDto) {
        log.debug("Converting customer to user: {}", customerDto.getCustomerNumber());
        
        UserDto userDto = new UserDto();
        userDto.setUserId(customerDto.getUserId());
        userDto.setEmail(customerDto.getCustomerEmailId());
        userDto.setPhoneNumber(customerDto.getCustomerMobileNumber());
        userDto.setUserType("CUSTOMER");
        userDto.setStatus(customerDto.getCustomerStatus());
        userDto.setCreatedAt(customerDto.getCreatedAt());
        userDto.setUpdatedAt(customerDto.getUpdatedAt());
        
        // Extract first and last name from name components
        List<CustomerNameComponentDto> nameComponents = 
            nameComponentService.getNameComponentsByCustomerNumber(customerDto.getCustomerNumber());
        
        for (CustomerNameComponentDto component : nameComponents) {
            if (CustomerNameComponent.FIRST_NAME.equals(component.getNameComponentType())) {
                userDto.setFirstName(component.getNameValue());
            } else if (CustomerNameComponent.LAST_NAME.equals(component.getNameComponentType())) {
                userDto.setLastName(component.getNameValue());
            }
        }
        
        // Fallback to parsing full name if name components not found
        if (userDto.getFirstName() == null || userDto.getLastName() == null) {
            String[] nameParts = customerDto.getCustomerFullName().split(" ", 2);
            if (nameParts.length >= 1) {
                userDto.setFirstName(nameParts[0]);
            }
            if (nameParts.length >= 2) {
                userDto.setLastName(nameParts[1]);
            } else {
                userDto.setLastName(""); // Default empty if no last name
            }
        }
        
        return userDto;
    }
    
    /**
     * Check if a customer exists for a given user ID
     */
    public boolean customerExistsForUser(String userId) {
        return customerDetailsService.existsByUserId(userId);
    }
    
    /**
     * Delete customer when user is deleted
     */
    public void deleteCustomerByUserId(String userId) {
        log.info("Deleting customer for userId: {}", userId);
        
        Optional<CustomerDetailsDto> customerOpt = customerDetailsService.getCustomerByUserId(userId);
        if (customerOpt.isPresent()) {
            customerDetailsService.deleteCustomer(customerOpt.get().getCustomerNumber());
            log.info("Customer deleted for userId: {}", userId);
        }
    }
    
    private void createNameComponentsFromUser(String customerNumber, UserDto userDto) {
        // Create first name component
        if (userDto.getFirstName() != null && !userDto.getFirstName().trim().isEmpty()) {
            CustomerNameComponentDto firstNameComponent = new CustomerNameComponentDto();
            firstNameComponent.setCustomerNumber(customerNumber);
            firstNameComponent.setNameComponentType(CustomerNameComponent.FIRST_NAME);
            firstNameComponent.setNameValue(userDto.getFirstName().trim());
            firstNameComponent.setEffectiveDate(LocalDateTime.now());
            nameComponentService.createNameComponent(firstNameComponent);
        }
        
        // Create last name component
        if (userDto.getLastName() != null && !userDto.getLastName().trim().isEmpty()) {
            CustomerNameComponentDto lastNameComponent = new CustomerNameComponentDto();
            lastNameComponent.setCustomerNumber(customerNumber);
            lastNameComponent.setNameComponentType(CustomerNameComponent.LAST_NAME);
            lastNameComponent.setNameValue(userDto.getLastName().trim());
            lastNameComponent.setEffectiveDate(LocalDateTime.now());
            nameComponentService.createNameComponent(lastNameComponent);
        }
    }
    
    private void updateNameComponentsFromUser(String customerNumber, UserDto userDto) {
        // Get existing name components
        List<CustomerNameComponentDto> existingComponents = 
            nameComponentService.getNameComponentsByCustomerNumber(customerNumber);
        
        // Update or create first name
        updateOrCreateNameComponent(customerNumber, CustomerNameComponent.FIRST_NAME, 
            userDto.getFirstName(), existingComponents);
        
        // Update or create last name
        updateOrCreateNameComponent(customerNumber, CustomerNameComponent.LAST_NAME, 
            userDto.getLastName(), existingComponents);
    }
    
    private void updateOrCreateNameComponent(String customerNumber, String componentType, 
            String newValue, List<CustomerNameComponentDto> existingComponents) {
        
        if (newValue == null || newValue.trim().isEmpty()) {
            return;
        }
        
        // Find existing component
        Optional<CustomerNameComponentDto> existingComponent = existingComponents.stream()
            .filter(comp -> componentType.equals(comp.getNameComponentType()))
            .findFirst();
        
        if (existingComponent.isPresent()) {
            // Update existing
            CustomerNameComponentDto component = existingComponent.get();
            component.setNameValue(newValue.trim());
            nameComponentService.updateNameComponent(component.getId(), component);
        } else {
            // Create new
            CustomerNameComponentDto newComponent = new CustomerNameComponentDto();
            newComponent.setCustomerNumber(customerNumber);
            newComponent.setNameComponentType(componentType);
            newComponent.setNameValue(newValue.trim());
            newComponent.setEffectiveDate(LocalDateTime.now());
            nameComponentService.createNameComponent(newComponent);
        }
    }
}
