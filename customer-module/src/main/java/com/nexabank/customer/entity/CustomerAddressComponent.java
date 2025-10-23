package com.nexabank.customer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_address_components")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddressComponent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "address_component_type", nullable = false)
    private AddressComponentType addressComponentType;
    
    @Column(name = "address_value", nullable = false)
    private String addressValue;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationship with Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
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
    
    // Common address component types as constants
    public static final String ADDRESS_LINE_1 = "ADDRESS_LINE_1";
    public static final String ADDRESS_LINE_2 = "ADDRESS_LINE_2";
    public static final String CITY = "CITY";
    public static final String STATE = "STATE";
    public static final String COUNTRY = "COUNTRY";
    public static final String POSTAL_CODE = "POSTAL_CODE";
    
    public enum AddressComponentType {
        ADDRESS_LINE_1, ADDRESS_LINE_2, CITY, STATE, COUNTRY, POSTAL_CODE
    }
}
