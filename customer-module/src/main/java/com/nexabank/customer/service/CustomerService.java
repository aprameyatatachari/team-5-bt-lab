package com.nexabank.customer.service;

import com.nexabank.customer.entity.Customer;
import com.nexabank.customer.entity.enums.CrudValue;
import com.nexabank.customer.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CustomerService {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    /**
     * Create a new customer profile
     */
    public Customer createCustomer(Customer customer) {
        // Set audit fields
        customer.setCrudValue(CrudValue.CREATE);
        customer.setUserId(customer.getUserId()); // Use customer's userId for audit
        customer.setUuidReference(UUID.randomUUID());
        
        // Generate customer number if not provided
        if (customer.getCustomerNumber() == null || customer.getCustomerNumber().isEmpty()) {
            customer.setCustomerNumber(generateCustomerNumber());
        }
        
        return customerRepository.save(customer);
    }
    
    /**
     * Create customer from registration data
     */
    public Customer createCustomerFromRegistration(String userId, String firstName, String lastName, 
                                                 String emailId, String phoneNumber) {
        Customer customer = new Customer();
        customer.setUserId(userId);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmailId(emailId);
        customer.setPhoneNumber(phoneNumber);
        customer.setCustomerType(Customer.CustomerType.INDIVIDUAL);
        customer.setCustomerStatus(Customer.CustomerStatus.ACTIVE);
        customer.setKycStatus(Customer.KycStatus.PENDING);
        customer.setRiskCategory(Customer.RiskCategory.LOW);
        
        return createCustomer(customer);
    }
    
    /**
     * Update customer profile
     */
    public Customer updateCustomer(Customer customer) {
        customer.setCrudValue(CrudValue.UPDATE);
        customer.setUuidReference(UUID.randomUUID());
        return customerRepository.save(customer);
    }
    
    /**
     * Find customer by ID
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findById(String customerId) {
        return customerRepository.findById(customerId);
    }
    
    /**
     * Find customer by user ID (from auth module)
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByUserId(String userId) {
        return customerRepository.findByUserId(userId);
    }
    
    /**
     * Find customer by email
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByEmail(String emailId) {
        return customerRepository.findByEmailId(emailId);
    }
    
    /**
     * Find customer by customer number
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByCustomerNumber(String customerNumber) {
        return customerRepository.findByCustomerNumber(customerNumber);
    }
    
    /**
     * Get all customers
     */
    @Transactional(readOnly = true)
    public List<Customer> findAllCustomers() {
        return customerRepository.findAll();
    }
    
    /**
     * Get active customers
     */
    @Transactional(readOnly = true)
    public List<Customer> findActiveCustomers() {
        return customerRepository.findAllActiveCustomers();
    }
    
    /**
     * Get customers by status
     */
    @Transactional(readOnly = true)
    public List<Customer> findByStatus(Customer.CustomerStatus status) {
        return customerRepository.findByCustomerStatus(status);
    }
    
    /**
     * Get customers requiring KYC
     */
    @Transactional(readOnly = true)
    public List<Customer> findCustomersRequiringKyc() {
        return customerRepository.findCustomersRequiringKyc();
    }
    
    /**
     * Search customers by name
     */
    @Transactional(readOnly = true)
    public List<Customer> searchByName(String name) {
        return customerRepository.findByNameContainingIgnoreCase(name);
    }
    
    /**
     * Update customer KYC status
     */
    public Customer updateKycStatus(String customerId, Customer.KycStatus kycStatus) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            customer.setKycStatus(kycStatus);
            if (kycStatus == Customer.KycStatus.COMPLETED) {
                customer.setKycCompletionDate(LocalDateTime.now());
            }
            return updateCustomer(customer);
        }
        throw new RuntimeException("Customer not found with ID: " + customerId);
    }
    
    /**
     * Update customer status
     */
    public Customer updateCustomerStatus(String customerId, Customer.CustomerStatus status) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            customer.setCustomerStatus(status);
            return updateCustomer(customer);
        }
        throw new RuntimeException("Customer not found with ID: " + customerId);
    }
    
    /**
     * Update customer risk category
     */
    public Customer updateRiskCategory(String customerId, Customer.RiskCategory riskCategory) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            customer.setRiskCategory(riskCategory);
            return updateCustomer(customer);
        }
        throw new RuntimeException("Customer not found with ID: " + customerId);
    }
    
    /**
     * Check if customer exists by user ID
     */
    @Transactional(readOnly = true)
    public boolean existsByUserId(String userId) {
        return customerRepository.existsByUserId(userId);
    }
    
    /**
     * Check if email exists
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String emailId) {
        return customerRepository.existsByEmailId(emailId);
    }
    
    /**
     * Check if Aadhar number exists
     */
    @Transactional(readOnly = true)
    public boolean existsByAadhar(String aadharNumber) {
        return customerRepository.existsByAadharNumber(aadharNumber);
    }
    
    /**
     * Check if PAN number exists
     */
    @Transactional(readOnly = true)
    public boolean existsByPan(String panNumber) {
        return customerRepository.existsByPanNumber(panNumber);
    }
    
    /**
     * Delete customer (soft delete by changing status)
     */
    public void deleteCustomer(String customerId) {
        updateCustomerStatus(customerId, Customer.CustomerStatus.CLOSED);
    }
    
    /**
     * Generate unique customer number
     */
    private String generateCustomerNumber() {
        // Generate customer number in format: CUST-YYYYMMDD-XXXXXX
        String prefix = "CUST";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5); // Last 8 digits
        return prefix + "-" + timestamp;
    }
    
    /**
     * Get customer statistics
     */
    @Transactional(readOnly = true)
    public CustomerStats getCustomerStats() {
        CustomerStats stats = new CustomerStats();
        stats.setTotalCustomers(customerRepository.count());
        stats.setActiveCustomers(customerRepository.countByCustomerStatus(Customer.CustomerStatus.ACTIVE));
        stats.setInactiveCustomers(customerRepository.countByCustomerStatus(Customer.CustomerStatus.INACTIVE));
        stats.setPendingKyc(customerRepository.findCustomersRequiringKyc().size());
        return stats;
    }
    
    /**
     * Inner class for customer statistics
     */
    public static class CustomerStats {
        private long totalCustomers;
        private long activeCustomers;
        private long inactiveCustomers;
        private long pendingKyc;
        
        // Getters and setters
        public long getTotalCustomers() { return totalCustomers; }
        public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }
        
        public long getActiveCustomers() { return activeCustomers; }
        public void setActiveCustomers(long activeCustomers) { this.activeCustomers = activeCustomers; }
        
        public long getInactiveCustomers() { return inactiveCustomers; }
        public void setInactiveCustomers(long inactiveCustomers) { this.inactiveCustomers = inactiveCustomers; }
        
        public long getPendingKyc() { return pendingKyc; }
        public void setPendingKyc(long pendingKyc) { this.pendingKyc = pendingKyc; }
    }
}