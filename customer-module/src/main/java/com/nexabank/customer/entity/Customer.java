package com.nexabank.customer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "customers")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends AuditLoggable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "customer_id", updatable = false, nullable = false)
    private String customerId;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "gender")
    private String gender;
    
    @Column(name = "nationality")
    private String nationality = "Indian";
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "alternate_phone")
    private String alternatePhone;
    
    @Column(name = "email_id")
    private String emailId;
    
    @Column(name = "address_line_1")
    private String addressLine1;
    
    @Column(name = "address_line_2")
    private String addressLine2;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "state")
    private String state;
    
    @Column(name = "country")
    private String country = "India";
    
    @Column(name = "postal_code")
    private String postalCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    private CustomerType customerType = CustomerType.INDIVIDUAL;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_status", nullable = false)
    private CustomerStatus customerStatus = CustomerStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    private KycStatus kycStatus = KycStatus.PENDING;
    
    @Column(name = "kyc_completion_date")
    private LocalDateTime kycCompletionDate;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CustomerIdentification> identificationDocuments;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CustomerNameComponent> nameComponents;
    
    // Utility methods to work with normalized data
    
    /**
     * Get first name from name components
     */
    public String getFirstName() {
        if (nameComponents == null) return null;
        return nameComponents.stream()
            .filter(nc -> CustomerNameComponent.NameComponentType.FIRST_NAME.equals(nc.getNameComponentType()))
            .map(CustomerNameComponent::getNameValue)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get last name from name components
     */
    public String getLastName() {
        if (nameComponents == null) return null;
        return nameComponents.stream()
            .filter(nc -> CustomerNameComponent.NameComponentType.LAST_NAME.equals(nc.getNameComponentType()))
            .map(CustomerNameComponent::getNameValue)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get middle name from name components
     */
    public String getMiddleName() {
        if (nameComponents == null) return null;
        return nameComponents.stream()
            .filter(nc -> CustomerNameComponent.MIDDLE_NAME.equals(nc.getNameComponentType()))
            .map(CustomerNameComponent::getNameValue)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get full name from name components
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        String firstName = getFirstName();
        String middleName = getMiddleName();
        String lastName = getLastName();
        
        if (firstName != null) fullName.append(firstName);
        if (middleName != null && !middleName.trim().isEmpty()) {
            fullName.append(" ").append(middleName);
        }
        if (lastName != null) fullName.append(" ").append(lastName);
        return fullName.toString().trim();
    }
    
    /**
     * Get Aadhar number from identification documents
     */
    public String getAadharNumber() {
        if (identificationDocuments == null) return null;
        return identificationDocuments.stream()
            .filter(id -> CustomerIdentification.AADHAR_CARD.equals(id.getIdentificationType()))
            .map(CustomerIdentification::getIdentificationItem)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get PAN number from identification documents
     */
    public String getPanNumber() {
        if (identificationDocuments == null) return null;
        return identificationDocuments.stream()
            .filter(id -> CustomerIdentification.PAN_CARD.equals(id.getIdentificationType()))
            .map(CustomerIdentification::getIdentificationItem)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get passport number from identification documents
     */
    public String getPassportNumber() {
        if (identificationDocuments == null) return null;
        return identificationDocuments.stream()
            .filter(id -> CustomerIdentification.PASSPORT.equals(id.getIdentificationType()))
            .map(CustomerIdentification::getIdentificationItem)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get driving license from identification documents
     */
    public String getDrivingLicense() {
        if (identificationDocuments == null) return null;
        return identificationDocuments.stream()
            .filter(id -> CustomerIdentification.DRIVING_LICENSE.equals(id.getIdentificationType()))
            .map(CustomerIdentification::getIdentificationItem)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get masked Aadhar for display
     */
    public String getMaskedAadhar() {
        String aadhar = getAadharNumber();
        if (aadhar == null || aadhar.length() < 4) {
            return null;
        }
        return "XXXX-XXXX-" + aadhar.substring(aadhar.length() - 4);
    }
    
    /**
     * Get masked PAN for display
     */
    public String getMaskedPan() {
        String pan = getPanNumber();
        if (pan == null || pan.length() < 4) {
            return null;
        }
        return pan.substring(0, 2) + "XXXX" + pan.substring(pan.length() - 2);
    }
    
    public enum CustomerType {
        INDIVIDUAL, CORPORATE, NRI, MINOR, JOINT, TRUST, PARTNERSHIP
    }
    
    public enum CustomerStatus {
        ACTIVE, INACTIVE, SUSPENDED, BLOCKED, CLOSED, DORMANT
    }
    
    public enum KycStatus {
        PENDING, IN_PROGRESS, COMPLETED, REJECTED, EXPIRED
    }
}
