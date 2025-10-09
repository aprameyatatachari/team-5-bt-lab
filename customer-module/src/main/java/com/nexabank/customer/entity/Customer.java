package com.nexabank.customer.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "customers")
public class Customer extends AuditLoggable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "customer_id", updatable = false, nullable = false)
    private String customerId;
    
    // Foreign key to auth User entity
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;
    
    // Personal Information
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Column(name = "middle_name")
    private String middleName;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "gender")
    private String gender;
    
    @Column(name = "nationality")
    private String nationality = "Indian";
    
    // Contact Information
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "alternate_phone")
    private String alternatePhone;
    
    @Column(name = "email_id")
    private String emailId;
    
    // Address Information
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
    
    // Identification Documents
    @Column(name = "aadhar_number", unique = true)
    private String aadharNumber;
    
    @Column(name = "pan_number", unique = true)
    private String panNumber;
    
    @Column(name = "passport_number")
    private String passportNumber;
    
    @Column(name = "driving_license")
    private String drivingLicense;
    
    // Customer Type and Status
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    private CustomerType customerType = CustomerType.INDIVIDUAL;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_status", nullable = false)
    private CustomerStatus customerStatus = CustomerStatus.ACTIVE;
    
    // Banking Information
    @Column(name = "customer_number", unique = true)
    private String customerNumber;
    
    @Column(name = "branch_code")
    private String branchCode;
    
    @Column(name = "relationship_manager_id")
    private String relationshipManagerId;
    
    // KYC Information
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    private KycStatus kycStatus = KycStatus.PENDING;
    
    @Column(name = "kyc_completion_date")
    private LocalDateTime kycCompletionDate;
    
    @Column(name = "annual_income")
    private Double annualIncome;
    
    @Column(name = "occupation")
    private String occupation;
    
    @Column(name = "employer_name")
    private String employerName;
    
    // Risk and Compliance
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category")
    private RiskCategory riskCategory = RiskCategory.LOW;
    
    @Column(name = "pep_status")
    private Boolean pepStatus = false; // Politically Exposed Person
    
    @Column(name = "fatca_status")
    private Boolean fatcaStatus = false; // Foreign Account Tax Compliance Act
    
    // Relationships
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<BankAccount> bankAccounts;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CustomerIdentification> identificationDocuments;
    
    // Constructors
    public Customer() {}
    
    public Customer(String userId, String firstName, String lastName, String emailId) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailId = emailId;
    }
    
    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getMiddleName() {
        return middleName;
    }
    
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }
    
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getNationality() {
        return nationality;
    }
    
    public void setNationality(String nationality) {
        this.nationality = nationality;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getAlternatePhone() {
        return alternatePhone;
    }
    
    public void setAlternatePhone(String alternatePhone) {
        this.alternatePhone = alternatePhone;
    }
    
    public String getEmailId() {
        return emailId;
    }
    
    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }
    
    public String getAddressLine1() {
        return addressLine1;
    }
    
    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }
    
    public String getAddressLine2() {
        return addressLine2;
    }
    
    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getPostalCode() {
        return postalCode;
    }
    
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
    
    public String getAadharNumber() {
        return aadharNumber;
    }
    
    public void setAadharNumber(String aadharNumber) {
        this.aadharNumber = aadharNumber;
    }
    
    public String getPanNumber() {
        return panNumber;
    }
    
    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }
    
    public String getPassportNumber() {
        return passportNumber;
    }
    
    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber;
    }
    
    public String getDrivingLicense() {
        return drivingLicense;
    }
    
    public void setDrivingLicense(String drivingLicense) {
        this.drivingLicense = drivingLicense;
    }
    
    public CustomerType getCustomerType() {
        return customerType;
    }
    
    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }
    
    public CustomerStatus getCustomerStatus() {
        return customerStatus;
    }
    
    public void setCustomerStatus(CustomerStatus customerStatus) {
        this.customerStatus = customerStatus;
    }
    
    public String getCustomerNumber() {
        return customerNumber;
    }
    
    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }
    
    public String getBranchCode() {
        return branchCode;
    }
    
    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }
    
    public String getRelationshipManagerId() {
        return relationshipManagerId;
    }
    
    public void setRelationshipManagerId(String relationshipManagerId) {
        this.relationshipManagerId = relationshipManagerId;
    }
    
    public KycStatus getKycStatus() {
        return kycStatus;
    }
    
    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }
    
    public LocalDateTime getKycCompletionDate() {
        return kycCompletionDate;
    }
    
    public void setKycCompletionDate(LocalDateTime kycCompletionDate) {
        this.kycCompletionDate = kycCompletionDate;
    }
    
    public Double getAnnualIncome() {
        return annualIncome;
    }
    
    public void setAnnualIncome(Double annualIncome) {
        this.annualIncome = annualIncome;
    }
    
    public String getOccupation() {
        return occupation;
    }
    
    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }
    
    public String getEmployerName() {
        return employerName;
    }
    
    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }
    
    public RiskCategory getRiskCategory() {
        return riskCategory;
    }
    
    public void setRiskCategory(RiskCategory riskCategory) {
        this.riskCategory = riskCategory;
    }
    
    public Boolean getPepStatus() {
        return pepStatus;
    }
    
    public void setPepStatus(Boolean pepStatus) {
        this.pepStatus = pepStatus;
    }
    
    public Boolean getFatcaStatus() {
        return fatcaStatus;
    }
    
    public void setFatcaStatus(Boolean fatcaStatus) {
        this.fatcaStatus = fatcaStatus;
    }
    
    public Set<BankAccount> getBankAccounts() {
        return bankAccounts;
    }
    
    public void setBankAccounts(Set<BankAccount> bankAccounts) {
        this.bankAccounts = bankAccounts;
    }
    
    public Set<CustomerIdentification> getIdentificationDocuments() {
        return identificationDocuments;
    }
    
    public void setIdentificationDocuments(Set<CustomerIdentification> identificationDocuments) {
        this.identificationDocuments = identificationDocuments;
    }
    
    // Utility method to get full name
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null) fullName.append(firstName);
        if (middleName != null && !middleName.trim().isEmpty()) {
            fullName.append(" ").append(middleName);
        }
        if (lastName != null) fullName.append(" ").append(lastName);
        return fullName.toString().trim();
    }
    
    // Enums
    public enum CustomerType {
        INDIVIDUAL, CORPORATE, NRI, MINOR, JOINT, TRUST, PARTNERSHIP
    }
    
    public enum CustomerStatus {
        ACTIVE, INACTIVE, SUSPENDED, BLOCKED, CLOSED, DORMANT
    }
    
    public enum KycStatus {
        PENDING, IN_PROGRESS, COMPLETED, REJECTED, EXPIRED
    }
    
    public enum RiskCategory {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }
}