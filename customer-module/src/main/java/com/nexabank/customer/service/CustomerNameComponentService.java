package com.nexabank.customer.service;

import com.nexabank.customer.entity.CustomerNameComponent;
import com.nexabank.customer.repository.CustomerNameComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerNameComponentService {
    
    @Autowired
    private CustomerNameComponentRepository nameComponentRepository;
    
    /**
     * Save a name component
     */
    public CustomerNameComponent save(CustomerNameComponent nameComponent) {
        return nameComponentRepository.save(nameComponent);
    }
    
    /**
     * Find name components by customer number
     */
    @Transactional(readOnly = true)
    public List<CustomerNameComponent> findByCustomerCustomerNumber(String customerNumber) {
        return nameComponentRepository.findByCustomerCustomerNumber(customerNumber);
    }
    
    /**
     * Find name components by name component type
     */
    @Transactional(readOnly = true)
    public List<CustomerNameComponent> findByNameComponentType(String nameComponentType) {
        return nameComponentRepository.findByNameComponentType(nameComponentType);
    }
    
    /**
     * Find by ID
     */
    @Transactional(readOnly = true)
    public Optional<CustomerNameComponent> findById(String id) {
        return nameComponentRepository.findById(id);
    }
    
    /**
     * Delete name components by customer number
     */
    public void deleteByCustomerCustomerNumber(String customerNumber) {
        nameComponentRepository.deleteByCustomerCustomerNumber(customerNumber);
    }
    
    /**
     * Delete name component by ID
     */
    public void deleteById(String id) {
        nameComponentRepository.deleteById(id);
    }
    
    /**
     * Update name component
     */
    public CustomerNameComponent update(CustomerNameComponent nameComponent) {
        nameComponent.setUpdatedAt(LocalDateTime.now());
        return nameComponentRepository.save(nameComponent);
    }
}