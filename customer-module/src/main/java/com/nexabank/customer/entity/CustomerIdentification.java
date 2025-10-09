package com.nexabank.customer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_identification")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerIdentification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "identification_id", updatable = false, nullable = false)
    private String identificationId;
    
    @Column(name = "identification_type", nullable = false)
    private String identificationType;
    
    @Column(name = "identification_item", nullable = false)
    private String identificationItem;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;
    
    // Relationship with Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_number", nullable = false)
    private Customer customer;
    
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
    
    // Common identification types as constants
    public static final String PAN_CARD = "PAN_CARD";
    public static final String AADHAR_CARD = "AADHAR_CARD";
    public static final String PASSPORT = "PASSPORT";
    public static final String VOTER_ID = "VOTER_ID";
    public static final String DRIVING_LICENSE = "DRIVING_LICENSE";
}
