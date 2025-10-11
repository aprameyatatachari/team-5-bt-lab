package com.nexabank.customer.service;

import com.nexabank.customer.entity.CustomerIdentification;
import com.nexabank.customer.repository.CustomerIdentificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerIdentificationService {
    
    @Autowired
    private CustomerIdentificationRepository identificationRepository;
    
    /**
     * Save an identification document
     */
    public CustomerIdentification save(CustomerIdentification identification) {
        return identificationRepository.save(identification);
    }
    
    /**
     * Find identification documents by customer number
     */
    @Transactional(readOnly = true)
    public List<CustomerIdentification> findByCustomerCustomerNumber(String customerNumber) {
        return identificationRepository.findByCustomerCustomerNumber(customerNumber);
    }
    
    /**
     * Find identification documents by type
     */
    @Transactional(readOnly = true)
    public List<CustomerIdentification> findByIdentificationType(String identificationType) {
        return identificationRepository.findByIdentificationType(identificationType);
    }
    
    /**
     * Find by ID
     */
    @Transactional(readOnly = true)
    public Optional<CustomerIdentification> findById(String id) {
        return identificationRepository.findById(id);
    }
    
    /**
     * Delete identification documents by customer number
     */
    public void deleteByCustomerCustomerNumber(String customerNumber) {
        identificationRepository.deleteByCustomerCustomerNumber(customerNumber);
    }
    
    /**
     * Delete identification document by ID
     */
    public void deleteById(String id) {
        identificationRepository.deleteById(id);
    }
    
    /**
     * Update identification document
     */
    public CustomerIdentification update(CustomerIdentification identification) {
        identification.setUpdatedAt(LocalDateTime.now());
        return identificationRepository.save(identification);
    }
}