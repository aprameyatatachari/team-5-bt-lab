package com.nexabank.customer.repository;

import com.nexabank.customer.entity.CustomerProofOfIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerProofOfIdentityRepository extends JpaRepository<CustomerProofOfIdentity, String> {
    
    List<CustomerProofOfIdentity> findByCustomerNumber(String customerNumber);
    
    List<CustomerProofOfIdentity> findByProofOfIdType(String proofOfIdType);
    
    Optional<CustomerProofOfIdentity> findByCustomerNumberAndProofOfIdType(String customerNumber, String proofOfIdType);
    
    @Query("SELECT c FROM CustomerProofOfIdentity c WHERE c.customerNumber = :customerNumber ORDER BY c.effectiveDate DESC")
    List<CustomerProofOfIdentity> findByCustomerNumberOrderByEffectiveDateDesc(@Param("customerNumber") String customerNumber);
    
    @Query("SELECT c FROM CustomerProofOfIdentity c WHERE c.endDate IS NULL OR c.endDate >= :currentDate")
    List<CustomerProofOfIdentity> findActiveProofOfIdentity(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT c FROM CustomerProofOfIdentity c WHERE c.customerNumber = :customerNumber AND (c.endDate IS NULL OR c.endDate >= :currentDate)")
    List<CustomerProofOfIdentity> findActiveProofOfIdentityByCustomerNumber(@Param("customerNumber") String customerNumber, @Param("currentDate") LocalDate currentDate);
    
    boolean existsByClassificationTypeValue(String classificationTypeValue);
    
    void deleteByCustomerNumber(String customerNumber);
}
