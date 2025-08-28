package com.nexabank.customer.repository;

import com.nexabank.customer.entity.CustomerClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerClassificationRepository extends JpaRepository<CustomerClassification, String> {
    
    List<CustomerClassification> findByClassificationType(String classificationType);
    
    Optional<CustomerClassification> findByClassificationTypeAndClassificationTypeValue(
            String classificationType, String classificationTypeValue);
    
    @Query("SELECT c FROM CustomerClassification c WHERE c.classificationType = :type ORDER BY c.effectiveDate DESC")
    List<CustomerClassification> findByClassificationTypeOrderByEffectiveDateDesc(@Param("type") String type);
    
    boolean existsByClassificationTypeAndClassificationTypeValue(String classificationType, String classificationTypeValue);
}
