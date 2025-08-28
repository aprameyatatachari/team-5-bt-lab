package com.nexabank.customer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "customer_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "customer_number", updatable = false, nullable = false)
    private String customerNumber;
    
    @Column(name = "customer_type", nullable = false)
    private String customerType;
    
    @Column(name = "customer_full_name", nullable = false)
    private String customerFullName;
    
    @Column(name = "customer_dob_doi")
    private LocalDate customerDobDoi;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_status", nullable = false)
    private CustomerStatus customerStatus = CustomerStatus.ACTIVE;
    
    @Column(name = "customer_contact_number")
    private String customerContactNumber;
    
    @Column(name = "customer_mobile_number")
    private String customerMobileNumber;
    
    @Column(name = "customer_email_id", unique = true)
    private String customerEmailId;
    
    @Column(name = "customer_country_of_origination")
    private String customerCountryOfOrigination = "India";
    
    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships with new entities
    @OneToMany(mappedBy = "customerNumber", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CustomerNameComponent> nameComponents;
    
    @OneToMany(mappedBy = "customerNumber", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CustomerProofOfIdentity> proofOfIdentities;
    
    // Link to existing User entity for authentication
    @Column(name = "user_id")
    private String userId;
    
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
    
    public enum CustomerStatus {
        ACTIVE, INACTIVE, SUSPENDED, BLOCKED, CLOSED
    }
    
    // Common customer types as constants
    public static final String INDIVIDUAL = "INDIVIDUAL";
    public static final String CORPORATE = "CORPORATE";
    public static final String NRI = "NRI";
    public static final String MINOR = "MINOR";
}
