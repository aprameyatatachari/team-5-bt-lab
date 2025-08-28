package com.nexabank.customer.repository;

import com.nexabank.customer.entity.CustomerDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerDetailsRepository extends JpaRepository<CustomerDetails, String> {
    
    Optional<CustomerDetails> findByCustomerNumber(String customerNumber);
    
    Optional<CustomerDetails> findByCustomerEmailId(String customerEmailId);
    
    Optional<CustomerDetails> findByUserId(String userId);
    
    List<CustomerDetails> findByCustomerType(String customerType);
    
    List<CustomerDetails> findByCustomerStatus(CustomerDetails.CustomerStatus customerStatus);
    
    Optional<CustomerDetails> findByCustomerMobileNumber(String customerMobileNumber);
    
    @Query("SELECT c FROM CustomerDetails c WHERE c.customerFullName LIKE %:name%")
    List<CustomerDetails> findByCustomerFullNameContaining(@Param("name") String name);
    
    @Query("SELECT c FROM CustomerDetails c WHERE c.customerCountryOfOrigination = :country")
    List<CustomerDetails> findByCustomerCountryOfOrigination(@Param("country") String country);
    
    boolean existsByCustomerNumber(String customerNumber);
    
    boolean existsByCustomerEmailId(String customerEmailId);
    
    boolean existsByUserId(String userId);
    
    boolean existsByCustomerMobileNumber(String customerMobileNumber);
    
    @Query("SELECT COUNT(c) FROM CustomerDetails c WHERE c.customerType = :type")
    long countByCustomerType(@Param("type") String type);
}
