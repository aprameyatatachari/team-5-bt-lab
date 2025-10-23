package com.nexabank.customer.repository;

import com.nexabank.customer.entity.CustomerAddressComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressComponentRepository extends JpaRepository<CustomerAddressComponent, String> {
    
    List<CustomerAddressComponent> findByCustomerCustomerId(String customerId);
    
    List<CustomerAddressComponent> findByAddressComponentType(CustomerAddressComponent.AddressComponentType addressComponentType);
    
    Optional<CustomerAddressComponent> findByCustomerCustomerIdAndAddressComponentType(String customerId, CustomerAddressComponent.AddressComponentType addressComponentType);
    
    @Query("SELECT c FROM CustomerAddressComponent c WHERE c.customer.customerId = :customerId ORDER BY c.effectiveDate DESC")
    List<CustomerAddressComponent> findByCustomerIdOrderByEffectiveDateDesc(@Param("customerId") String customerId);
    
    @Query("SELECT c FROM CustomerAddressComponent c WHERE c.addressValue LIKE %:addressValue%")
    List<CustomerAddressComponent> findByAddressValueContaining(@Param("addressValue") String addressValue);
    
    void deleteByCustomerCustomerId(String customerId);
}
