package com.nexabank.customer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_name_components")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerNameComponent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "name_component_type", nullable = false)
    private NameComponentType nameComponentType;
    
    @Column(name = "name_value", nullable = false)
    private String nameValue;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // INSERT-ONLY audit trail fields
    @Enumerated(EnumType.STRING)
    @Column(name = "crud_operation", nullable = false)
    private CrudOperation crudOperation = CrudOperation.C;
    
    @Column(name = "version_timestamp", nullable = false)
    private LocalDateTime versionTimestamp;
    
    // Relationship with Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Business identifier to link components across versions (INSERT-ONLY)
    @Column(name = "customer_number", nullable = false)
    private String customerNumber;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (effectiveDate == null) {
            effectiveDate = now;
        }
        if (versionTimestamp == null) {
            versionTimestamp = now;
        }
        if (crudOperation == null) {
            crudOperation = CrudOperation.C;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (versionTimestamp == null) {
            versionTimestamp = LocalDateTime.now();
        }
    }
    
    // Common name component types as constants
    public static final String FIRST_NAME = "FIRST_NAME";
    public static final String MIDDLE_NAME = "MIDDLE_NAME";
    public static final String LAST_NAME = "LAST_NAME";
    public static final String MAIDEN_NAME = "MAIDEN_NAME";
    public static final String SUFFIX = "SUFFIX";
    public static final String PREFIX = "PREFIX";
    
    public enum NameComponentType {
        FIRST_NAME, MIDDLE_NAME, LAST_NAME, MAIDEN_NAME, SUFFIX, PREFIX
    }
    
    // CRUD Operation enum for INSERT-ONLY paradigm
    public enum CrudOperation {
        C, // Create
        U, // Update
        D  // Delete
    }
}
