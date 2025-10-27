package com.nexabank.customer.repository;

import com.nexabank.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    
    // Find customer by user ID (from auth module)
    Optional<Customer> findByUserId(String userId);
    
    // Find customer by email
    Optional<Customer> findByEmailId(String emailId);
    
    // Find customer by Aadhar number - using join with CustomerIdentification
    @Query("SELECT c FROM Customer c JOIN c.identificationDocuments id WHERE id.identificationType = 'AADHAR_CARD' AND id.identificationItem = :aadharNumber")
    Optional<Customer> findByAadharNumber(@Param("aadharNumber") String aadharNumber);
    
    // Find customer by PAN number - using join with CustomerIdentification
    @Query("SELECT c FROM Customer c JOIN c.identificationDocuments id WHERE id.identificationType = 'PAN_CARD' AND id.identificationItem = :panNumber")
    Optional<Customer> findByPanNumber(@Param("panNumber") String panNumber);
    
    // Find customers by status (latest non-deleted versions only)
    @Query("SELECT c FROM Customer c WHERE c.customerStatus = :status AND c.crudOperation != 'D' " +
           "AND c.customerId IN (SELECT MAX(c2.customerId) FROM Customer c2 WHERE c2.customerNumber = c.customerNumber)")
    List<Customer> findByCustomerStatus(@Param("status") Customer.CustomerStatus status);
    
    // Find customers by type (latest non-deleted versions only)
    @Query("SELECT c FROM Customer c WHERE c.customerType = :type AND c.crudOperation != 'D' " +
           "AND c.customerId IN (SELECT MAX(c2.customerId) FROM Customer c2 WHERE c2.customerNumber = c.customerNumber)")
    List<Customer> findByCustomerType(@Param("type") Customer.CustomerType type);
    
    // Find customers by KYC status (latest non-deleted versions only)
    @Query("SELECT c FROM Customer c WHERE c.kycStatus = :kycStatus AND c.crudOperation != 'D' " +
           "AND c.customerId IN (SELECT MAX(c2.customerId) FROM Customer c2 WHERE c2.customerNumber = c.customerNumber)")
    List<Customer> findByKycStatus(@Param("kycStatus") Customer.KycStatus kycStatus);
    
    // Search customers by name (case insensitive) - latest non-deleted versions only
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.nameComponents nc WHERE " +
           "LOWER(nc.nameValue) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "AND c.crudOperation != 'D' " +
           "AND c.customerId IN (SELECT MAX(c2.customerId) FROM Customer c2 WHERE c2.customerNumber = c.customerNumber)")
    List<Customer> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Find customers with phone number (latest non-deleted versions only)
    @Query("SELECT c FROM Customer c WHERE c.phoneNumber LIKE CONCAT('%', :phoneNumber, '%') " +
           "AND c.crudOperation != 'D' " +
           "AND c.customerId IN (SELECT MAX(c2.customerId) FROM Customer c2 WHERE c2.customerNumber = c.customerNumber)")
    List<Customer> findByPhoneNumberContaining(@Param("phoneNumber") String phoneNumber);
    
    // Find active customers (latest non-deleted versions only)
    @Query("SELECT c FROM Customer c WHERE c.customerStatus = 'ACTIVE' " +
           "AND c.crudOperation != 'D' " +
           "AND c.customerId IN (SELECT MAX(c2.customerId) FROM Customer c2 WHERE c2.customerNumber = c.customerNumber)")
    List<Customer> findAllActiveCustomers();
    
    // Find customers requiring KYC completion (latest non-deleted versions only)
    @Query("SELECT c FROM Customer c WHERE c.kycStatus IN ('PENDING', 'IN_PROGRESS') " +
           "AND c.crudOperation != 'D' " +
           "AND c.customerId IN (SELECT MAX(c2.customerId) FROM Customer c2 WHERE c2.customerNumber = c.customerNumber)")
    List<Customer> findCustomersRequiringKyc();
    
    // Count customers by status (latest non-deleted versions only)
    @Query("SELECT COUNT(DISTINCT c.customerNumber) FROM Customer c WHERE c.customerStatus = :status " +
           "AND c.crudOperation != 'D' " +
           "AND c.customerId IN (SELECT MAX(c2.customerId) FROM Customer c2 WHERE c2.customerNumber = c.customerNumber)")
    long countByCustomerStatus(@Param("status") Customer.CustomerStatus status);
    
    // Check if customer exists by user ID (non-deleted)
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.userId = :userId AND c.crudOperation != 'D'")
    boolean existsByUserId(@Param("userId") String userId);
    
    // Check if email exists (non-deleted)
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.emailId = :emailId AND c.crudOperation != 'D'")
    boolean existsByEmailId(@Param("emailId") String emailId);
    
    // Check if Aadhar number exists - using join with CustomerIdentification (non-deleted)
    @Query("SELECT COUNT(c) > 0 FROM Customer c JOIN c.identificationDocuments id " +
           "WHERE id.identificationType = 'AADHAR_CARD' AND id.identificationItem = :aadharNumber " +
           "AND c.crudOperation != 'D'")
    boolean existsByAadharNumber(@Param("aadharNumber") String aadharNumber);
    
    // Check if PAN number exists - using join with CustomerIdentification (non-deleted)
    @Query("SELECT COUNT(c) > 0 FROM Customer c JOIN c.identificationDocuments id " +
           "WHERE id.identificationType = 'PAN_CARD' AND id.identificationItem = :panNumber " +
           "AND c.crudOperation != 'D'")
    boolean existsByPanNumber(@Param("panNumber") String panNumber);
    
    // ============ INSERT-ONLY AUDIT TRAIL METHODS ============
    
    // Find latest version by customer number (excludes deleted records)
    @Query(value = "SELECT * FROM customers WHERE customer_number = :customerNumber " +
           "AND crud_operation != 'D' " +
           "ORDER BY version_timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<Customer> findLatestByCustomerNumber(@Param("customerNumber") String customerNumber);
    
    // Find latest version by user ID (excludes deleted records)
    @Query(value = "SELECT * FROM customers WHERE user_id = :userId " +
           "AND crud_operation != 'D' " +
           "ORDER BY version_timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<Customer> findLatestByUserId(@Param("userId") String userId);
    
    // Find latest version by email (excludes deleted records)
    @Query(value = "SELECT * FROM customers WHERE email_id = :emailId " +
           "AND crud_operation != 'D' " +
           "ORDER BY version_timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<Customer> findLatestByEmail(@Param("emailId") String emailId);
    
    // Find all versions of a customer by customer number
    @Query("SELECT c FROM Customer c WHERE c.customerNumber = :customerNumber " +
           "ORDER BY c.versionTimestamp DESC")
    List<Customer> findAllVersionsByCustomerNumber(@Param("customerNumber") String customerNumber);
    
    // Find customer number by user ID
    @Query("SELECT c.customerNumber FROM Customer c WHERE c.userId = :userId " +
           "ORDER BY c.versionTimestamp DESC")
    Optional<String> findCustomerNumberByUserId(@Param("userId") String userId);
    
    // Check if customer exists (not deleted) by customer number
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.customerNumber = :customerNumber " +
           "AND c.crudOperation != 'D'")
    boolean existsByCustomerNumber(@Param("customerNumber") String customerNumber);
    
    // Get all active (non-deleted) customers - latest versions only
    @Query("SELECT c FROM Customer c WHERE c.customerId IN " +
           "(SELECT MAX(c2.customerId) FROM Customer c2 " +
           "WHERE c2.crudOperation != 'D' " +
           "GROUP BY c2.customerNumber)")
    List<Customer> findAllLatestVersions();
}
