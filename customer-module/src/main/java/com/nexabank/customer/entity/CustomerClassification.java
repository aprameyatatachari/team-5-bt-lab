package com.nexabank.customer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_classification")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerClassification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "classification_id", updatable = false, nullable = false)
    private String classificationId;
    
    @Column(name = "classification_type", nullable = false)
    private String classificationType;
    
    @Column(name = "classification_type_value", nullable = false)
    private String classificationTypeValue;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (effectiveDate == null) {
            effectiveDate = now;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Common classification types as constants
    public static final String NAME_COMPONENT_TYPE = "NAME_COMPONENT_TYPE";
    public static final String PROOF_OF_ID_TYPE = "PROOF_OF_ID_TYPE";
    public static final String CUSTOMER_TYPE = "CUSTOMER_TYPE";
    public static final String IDENTIFICATION_TYPE = "IDENTIFICATION_TYPE";
}
