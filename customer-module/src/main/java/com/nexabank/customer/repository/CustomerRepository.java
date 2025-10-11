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
    
    // Find customer by customer number
    Optional<Customer> findByCustomerNumber(String customerNumber);
    
    // Find customer by Aadhar number - using join with CustomerIdentification
    @Query("SELECT c FROM Customer c JOIN c.identificationDocuments id WHERE id.identificationType = 'AADHAR_CARD' AND id.identificationItem = :aadharNumber")
    Optional<Customer> findByAadharNumber(@Param("aadharNumber") String aadharNumber);
    
    // Find customer by PAN number - using join with CustomerIdentification
    @Query("SELECT c FROM Customer c JOIN c.identificationDocuments id WHERE id.identificationType = 'PAN_CARD' AND id.identificationItem = :panNumber")
    Optional<Customer> findByPanNumber(@Param("panNumber") String panNumber);
    
    // Find customers by status
    List<Customer> findByCustomerStatus(Customer.CustomerStatus status);
    
    // Find customers by type
    List<Customer> findByCustomerType(Customer.CustomerType type);
    
    // Find customers by KYC status
    List<Customer> findByKycStatus(Customer.KycStatus kycStatus);
    
    // Find customers by risk category
    List<Customer> findByRiskCategory(Customer.RiskCategory riskCategory);
    
    // Find customers by branch code
    List<Customer> findByBranchCode(String branchCode);
    
    // Find customers by relationship manager
    List<Customer> findByRelationshipManagerId(String relationshipManagerId);
    
    // Search customers by name (case insensitive) - using join with CustomerNameComponent
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.nameComponents nc WHERE " +
           "LOWER(nc.nameValue) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Find customers with phone number
    List<Customer> findByPhoneNumberContaining(String phoneNumber);
    
    // Find active customers
    @Query("SELECT c FROM Customer c WHERE c.customerStatus = 'ACTIVE'")
    List<Customer> findAllActiveCustomers();
    
    // Find customers requiring KYC completion
    @Query("SELECT c FROM Customer c WHERE c.kycStatus IN ('PENDING', 'IN_PROGRESS')")
    List<Customer> findCustomersRequiringKyc();
    
    // Count customers by status
    long countByCustomerStatus(Customer.CustomerStatus status);
    
    // Check if customer exists by user ID
    boolean existsByUserId(String userId);
    
    // Check if email exists
    boolean existsByEmailId(String emailId);
    
    // Check if Aadhar number exists - using join with CustomerIdentification
    @Query("SELECT COUNT(c) > 0 FROM Customer c JOIN c.identificationDocuments id WHERE id.identificationType = 'AADHAR_CARD' AND id.identificationItem = :aadharNumber")
    boolean existsByAadharNumber(@Param("aadharNumber") String aadharNumber);
    
    // Check if PAN number exists - using join with CustomerIdentification
    @Query("SELECT COUNT(c) > 0 FROM Customer c JOIN c.identificationDocuments id WHERE id.identificationType = 'PAN_CARD' AND id.identificationItem = :panNumber")
    boolean existsByPanNumber(@Param("panNumber") String panNumber);
}