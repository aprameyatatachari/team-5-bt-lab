package com.nexabank.customer.service;

import com.nexabank.customer.dto.CustomerDetailsDto;
import com.nexabank.customer.entity.CustomerDetails;
import com.nexabank.customer.mapper.CustomerMapper;
import com.nexabank.customer.repository.CustomerDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerDetailsService {
    
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerMapper customerMapper;
    
    public CustomerDetailsDto createCustomer(CustomerDetailsDto customerDto) {
        log.info("Creating new customer: {}", customerDto.getCustomerFullName());
        
        CustomerDetails customer = customerMapper.toEntity(customerDto);
        customer.setEffectiveDate(LocalDateTime.now());
        
        CustomerDetails savedCustomer = customerDetailsRepository.save(customer);
        log.info("Customer created with number: {}", savedCustomer.getCustomerNumber());
        
        return customerMapper.toDto(savedCustomer);
    }
    
    public Optional<CustomerDetailsDto> getCustomerByNumber(String customerNumber) {
        log.info("Fetching customer by number: {}", customerNumber);
        
        return customerDetailsRepository.findByCustomerNumber(customerNumber)
                .map(customerMapper::toDto);
    }
    
    public Optional<CustomerDetailsDto> getCustomerByEmail(String email) {
        log.info("Fetching customer by email: {}", email);
        
        return customerDetailsRepository.findByCustomerEmailId(email)
                .map(customerMapper::toDto);
    }
    
    public Optional<CustomerDetailsDto> getCustomerByUserId(String userId) {
        log.info("Fetching customer by userId: {}", userId);
        
        return customerDetailsRepository.findByUserId(userId)
                .map(customerMapper::toDto);
    }
    
    public List<CustomerDetailsDto> getCustomersByStatus(String status) {
        log.info("Fetching customers by status: {}", status);
        
        CustomerDetails.CustomerStatus customerStatus = CustomerDetails.CustomerStatus.valueOf(status);
        return customerDetailsRepository.findByCustomerStatus(customerStatus)
                .stream()
                .map(customerMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public CustomerDetailsDto updateCustomer(String customerNumber, CustomerDetailsDto customerDto) {
        log.info("Updating customer: {}", customerNumber);
        
        CustomerDetails existingCustomer = customerDetailsRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerNumber));
        
        // Update fields
        existingCustomer.setCustomerType(customerDto.getCustomerType());
        existingCustomer.setCustomerFullName(customerDto.getCustomerFullName());
        existingCustomer.setCustomerDobDoi(customerDto.getCustomerDobDoi());
        existingCustomer.setCustomerStatus(CustomerDetails.CustomerStatus.valueOf(customerDto.getCustomerStatus()));
        existingCustomer.setCustomerContactNumber(customerDto.getCustomerContactNumber());
        existingCustomer.setCustomerMobileNumber(customerDto.getCustomerMobileNumber());
        existingCustomer.setCustomerEmailId(customerDto.getCustomerEmailId());
        existingCustomer.setCustomerCountryOfOrigination(customerDto.getCustomerCountryOfOrigination());
        
        CustomerDetails updatedCustomer = customerDetailsRepository.save(existingCustomer);
        log.info("Customer updated: {}", customerNumber);
        
        return customerMapper.toDto(updatedCustomer);
    }
    
    public void deleteCustomer(String customerNumber) {
        log.info("Deleting customer: {}", customerNumber);
        
        CustomerDetails customer = customerDetailsRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerNumber));
        
        customerDetailsRepository.delete(customer);
        log.info("Customer deleted: {}", customerNumber);
    }
    
    public List<CustomerDetailsDto> getAllCustomers() {
        log.info("Fetching all customers");
        
        return customerDetailsRepository.findAll()
                .stream()
                .map(customerMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public List<CustomerDetailsDto> getCustomersByType(String customerType) {
        log.info("Fetching customers by type: {}", customerType);
        
        return customerDetailsRepository.findByCustomerType(customerType)
                .stream()
                .map(customerMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public boolean existsByEmail(String email) {
        return customerDetailsRepository.existsByCustomerEmailId(email);
    }
    
    public boolean existsByCustomerNumber(String customerNumber) {
        return customerDetailsRepository.existsByCustomerNumber(customerNumber);
    }
    
    public boolean existsByUserId(String userId) {
        return customerDetailsRepository.existsByUserId(userId);
    }
}
