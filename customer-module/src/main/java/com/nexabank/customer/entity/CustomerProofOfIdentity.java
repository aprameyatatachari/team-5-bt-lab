package com.nexabank.customer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_proof_of_identity")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProofOfIdentity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;
    
    @Column(name = "customer_number", nullable = false)
    private String customerNumber;
    
    @Column(name = "proof_of_id_type", nullable = false)
    private String proofOfIdType; // References CustomerClassification
    
    @Column(name = "classification_type_value", nullable = false)
    private String classificationTypeValue;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationship with Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_number", insertable = false, updatable = false)
    private Customer customer;
    
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
    
    // Common proof of identity types as constants
    public static final String PAN_CARD = "PAN_CARD";
    public static final String AADHAR_CARD = "AADHAR_CARD";
    public static final String PASSPORT = "PASSPORT";
    public static final String VOTER_ID = "VOTER_ID";
    public static final String DRIVING_LICENSE = "DRIVING_LICENSE";
    public static final String BANK_STATEMENT = "BANK_STATEMENT";
    public static final String UTILITY_BILL = "UTILITY_BILL";
}
