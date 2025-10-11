package com.nexabank.customer.repository;

import com.nexabank.customer.entity.CustomerIdentification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerIdentificationRepository extends JpaRepository<CustomerIdentification, String> {
    
    List<CustomerIdentification> findByIdentificationType(String identificationType);
    
    Optional<CustomerIdentification> findByIdentificationTypeAndIdentificationItem(
            String identificationType, String identificationItem);
    
    @Query("SELECT c FROM CustomerIdentification c WHERE c.identificationType = :type ORDER BY c.effectiveDate DESC")
    List<CustomerIdentification> findByIdentificationTypeOrderByEffectiveDateDesc(@Param("type") String type);
    
    boolean existsByIdentificationTypeAndIdentificationItem(String identificationType, String identificationItem);
    
    List<CustomerIdentification> findByIdentificationItem(String identificationItem);
    
    List<CustomerIdentification> findByCustomerCustomerNumber(String customerNumber);
    
    void deleteByCustomerCustomerNumber(String customerNumber);
}
