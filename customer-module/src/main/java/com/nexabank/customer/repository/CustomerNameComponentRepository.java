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
    
    List<CustomerNameComponent> findByCustomerNumber(String customerNumber);
    
    List<CustomerNameComponent> findByNameComponentType(String nameComponentType);
    
    Optional<CustomerNameComponent> findByCustomerNumberAndNameComponentType(String customerNumber, String nameComponentType);
    
    @Query("SELECT c FROM CustomerNameComponent c WHERE c.customerNumber = :customerNumber ORDER BY c.effectiveDate DESC")
    List<CustomerNameComponent> findByCustomerNumberOrderByEffectiveDateDesc(@Param("customerNumber") String customerNumber);
    
    @Query("SELECT c FROM CustomerNameComponent c WHERE c.nameValue LIKE %:nameValue%")
    List<CustomerNameComponent> findByNameValueContaining(@Param("nameValue") String nameValue);
    
    void deleteByCustomerNumber(String customerNumber);
}
