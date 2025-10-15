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
    
    List<CustomerNameComponent> findByNameComponentType(CustomerNameComponent.NameComponentType nameComponentType);
    
    Optional<CustomerNameComponent> findByCustomerCustomerIdAndNameComponentType(String customerId, CustomerNameComponent.NameComponentType nameComponentType);
    
    @Query("SELECT c FROM CustomerNameComponent c WHERE c.customer.customerId = :customerId ORDER BY c.effectiveDate DESC")
    List<CustomerNameComponent> findByCustomerIdOrderByEffectiveDateDesc(@Param("customerId") String customerId);
    
    @Query("SELECT c FROM CustomerNameComponent c WHERE c.nameValue LIKE %:nameValue%")
    List<CustomerNameComponent> findByNameValueContaining(@Param("nameValue") String nameValue);
    
    void deleteByCustomerCustomerId(String customerId);
}
