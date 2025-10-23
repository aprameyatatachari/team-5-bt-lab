package com.nexabank.customer.service;

import com.nexabank.customer.entity.Customer;
import com.nexabank.customer.entity.CustomerNameComponent;
import com.nexabank.customer.repository.CustomerRepository;
import com.nexabank.customer.repository.CustomerNameComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerService {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private CustomerNameComponentRepository nameComponentRepository;
    
    @Autowired
    private CustomerNumberGenerator customerNumberGenerator;
    
    /**
     * Create a new customer profile (INSERT-ONLY: Always creates new row with C operation)
     */
    public Customer createCustomer(Customer customer) {
        // Generate customer number for first version
        if (customer.getCustomerNumber() == null) {
            customer.setCustomerNumber(customerNumberGenerator.generateCustomerNumber());
        }
        
        // Set CRUD operation to CREATE
        customer.setCrudOperation(Customer.CrudOperation.C);
        customer.setVersionTimestamp(LocalDateTime.now());
        
        return customerRepository.save(customer);
    }
    
    /**
     * Create customer from registration data
     */
    public Customer createCustomerFromRegistration(String userId, String firstName, String lastName, 
                                                 String emailId, String phoneNumber) {
        Customer customer = new Customer();
        customer.setUserId(userId);
        customer.setEmailId(emailId);
        customer.setPhoneNumber(phoneNumber);
        customer.setCustomerType(Customer.CustomerType.INDIVIDUAL);
        customer.setCustomerStatus(Customer.CustomerStatus.ACTIVE);
        customer.setKycStatus(Customer.KycStatus.PENDING);
        
        // Save customer first to get the ID
        Customer savedCustomer = createCustomer(customer);
        
        // Create name components for the saved customer
        if (firstName != null && !firstName.trim().isEmpty()) {
            createNameComponent(savedCustomer, CustomerNameComponent.NameComponentType.FIRST_NAME, firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            createNameComponent(savedCustomer, CustomerNameComponent.NameComponentType.LAST_NAME, lastName.trim());
        }
        
        return savedCustomer;
    }
    
    /**
     * Helper method to create name components
     */
    private void createNameComponent(Customer customer, CustomerNameComponent.NameComponentType nameType, String nameValue) {
        CustomerNameComponent nameComponent = new CustomerNameComponent();
        nameComponent.setCustomer(customer);
        nameComponent.setNameComponentType(nameType);
        nameComponent.setNameValue(nameValue);
        nameComponent.setEffectiveDate(LocalDateTime.now());
        nameComponentRepository.save(nameComponent);
    }
    
    /**
     * Update customer profile (INSERT-ONLY: Creates new row with U operation)
     */
    public Customer updateCustomer(Customer existingCustomer) {
        // Create a new row with same customer number
        Customer updatedCustomer = new Customer();
        
        // Copy customer number to maintain identity
        updatedCustomer.setCustomerNumber(existingCustomer.getCustomerNumber());
        updatedCustomer.setUserId(existingCustomer.getUserId());
        
        // Copy all fields from existing customer
        updatedCustomer.setDateOfBirth(existingCustomer.getDateOfBirth());
        updatedCustomer.setGender(existingCustomer.getGender());
        updatedCustomer.setNationality(existingCustomer.getNationality());
        updatedCustomer.setPhoneNumber(existingCustomer.getPhoneNumber());
        updatedCustomer.setAlternatePhone(existingCustomer.getAlternatePhone());
        updatedCustomer.setEmailId(existingCustomer.getEmailId());
        updatedCustomer.setAddressLine1(existingCustomer.getAddressLine1());
        updatedCustomer.setAddressLine2(existingCustomer.getAddressLine2());
        updatedCustomer.setCity(existingCustomer.getCity());
        updatedCustomer.setState(existingCustomer.getState());
        updatedCustomer.setCountry(existingCustomer.getCountry());
        updatedCustomer.setPostalCode(existingCustomer.getPostalCode());
        updatedCustomer.setCustomerType(existingCustomer.getCustomerType());
        updatedCustomer.setCustomerStatus(existingCustomer.getCustomerStatus());
        updatedCustomer.setKycStatus(existingCustomer.getKycStatus());
        updatedCustomer.setKycCompletionDate(existingCustomer.getKycCompletionDate());
        
        // Set CRUD operation to UPDATE
        updatedCustomer.setCrudOperation(Customer.CrudOperation.U);
        updatedCustomer.setVersionTimestamp(LocalDateTime.now());
        
        return customerRepository.save(updatedCustomer);
    }
    
    /**
     * Find customer by ID
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findById(String customerId) {
        return customerRepository.findById(customerId);
    }
    
    /**
     * Find customer by user ID (from auth module) - returns latest non-deleted version
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByUserId(String userId) {
        return customerRepository.findLatestByUserId(userId);
    }
    
    /**
     * Find customer by email
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByEmail(String emailId) {
        return customerRepository.findByEmailId(emailId);
    }
    
    /**
     * Get all customers - returns latest versions only (non-deleted)
     */
    @Transactional(readOnly = true)
    public List<Customer> findAllCustomers() {
        return customerRepository.findAllLatestVersions();
    }
    
    /**
     * Find all versions of a customer by customer number (for audit trail)
     */
    @Transactional(readOnly = true)
    public List<Customer> findAllVersionsByCustomerNumber(String customerNumber) {
        return customerRepository.findAllVersionsByCustomerNumber(customerNumber);
    }
    
    /**
     * Find customer by customer number - returns latest non-deleted version
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByCustomerNumber(String customerNumber) {
        return customerRepository.findLatestByCustomerNumber(customerNumber);
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
     * Check if customer exists by user ID (not deleted)
     */
    @Transactional(readOnly = true)
    public boolean existsByUserId(String userId) {
        return customerRepository.findLatestByUserId(userId).isPresent();
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
     * Delete customer (INSERT-ONLY: Creates new row with D operation - soft delete)
     */
    public void deleteCustomer(String customerId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isPresent()) {
            Customer existingCustomer = customerOpt.get();
            
            // Create new row with DELETE operation
            Customer deletedCustomer = new Customer();
            deletedCustomer.setCustomerNumber(existingCustomer.getCustomerNumber());
            deletedCustomer.setUserId(existingCustomer.getUserId());
            
            // Copy all fields
            deletedCustomer.setDateOfBirth(existingCustomer.getDateOfBirth());
            deletedCustomer.setGender(existingCustomer.getGender());
            deletedCustomer.setNationality(existingCustomer.getNationality());
            deletedCustomer.setPhoneNumber(existingCustomer.getPhoneNumber());
            deletedCustomer.setAlternatePhone(existingCustomer.getAlternatePhone());
            deletedCustomer.setEmailId(existingCustomer.getEmailId());
            deletedCustomer.setAddressLine1(existingCustomer.getAddressLine1());
            deletedCustomer.setAddressLine2(existingCustomer.getAddressLine2());
            deletedCustomer.setCity(existingCustomer.getCity());
            deletedCustomer.setState(existingCustomer.getState());
            deletedCustomer.setCountry(existingCustomer.getCountry());
            deletedCustomer.setPostalCode(existingCustomer.getPostalCode());
            deletedCustomer.setCustomerType(existingCustomer.getCustomerType());
            deletedCustomer.setCustomerStatus(Customer.CustomerStatus.CLOSED);
            deletedCustomer.setKycStatus(existingCustomer.getKycStatus());
            deletedCustomer.setKycCompletionDate(existingCustomer.getKycCompletionDate());
            
            // Set CRUD operation to DELETE
            deletedCustomer.setCrudOperation(Customer.CrudOperation.D);
            deletedCustomer.setVersionTimestamp(LocalDateTime.now());
            
            customerRepository.save(deletedCustomer);
        }
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