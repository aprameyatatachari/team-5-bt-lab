package com.nexabank.customer.repository;

import com.nexabank.customer.entity.CustomerNameComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerNameComponentRepository extends JpaRepository<CustomerNameComponent, String> {
    
    List<CustomerNameComponent> findByCustomerCustomerId(String customerId);
    
    // Find components by customer number (business id) - latest versions / ordering handled by caller
    List<CustomerNameComponent> findByCustomerNumber(String customerNumber);
    
    List<CustomerNameComponent> findByNameComponentType(CustomerNameComponent.NameComponentType nameComponentType);
    
    Optional<CustomerNameComponent> findByCustomerCustomerIdAndNameComponentType(String customerId, CustomerNameComponent.NameComponentType nameComponentType);
    
    @Query("SELECT c FROM CustomerNameComponent c WHERE c.customerNumber = :customerNumber AND c.crudOperation != 'D' ORDER BY c.versionTimestamp DESC")
    List<CustomerNameComponent> findByCustomerNumberOrderByVersionDesc(@Param("customerNumber") String customerNumber);
    
    @Query("SELECT c FROM CustomerNameComponent c WHERE c.nameValue LIKE %:nameValue%")
    List<CustomerNameComponent> findByNameValueContaining(@Param("nameValue") String nameValue);
    
    void deleteByCustomerCustomerId(String customerId);
    
    void deleteByCustomerNumber(String customerNumber);
}
