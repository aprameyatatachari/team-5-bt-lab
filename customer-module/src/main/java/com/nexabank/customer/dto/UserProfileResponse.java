package com.nexabank.customer.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for returning customer profile information with INSERT-ONLY paradigm fields
 */
public class UserProfileResponse {
    
    // INSERT-ONLY paradigm fields
    private String customerId;          // Technical ID (changes with each version)
    private String customerNumber;      // Business ID (same across all versions)
    private String crudOperation;       // C, U, or D
    private LocalDateTime versionTimestamp; // When this version was created
    
    // User linkage
    private String userId;
    
    // Personal information
    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    
    // Contact information
    private String phoneNumber;
    private String alternatePhone;
    
    // Address information
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    
    // Identification documents (masked for security)
    private String maskedAadhar;
    private String maskedPan;
    private String aadharNumber;        // Full number (only for authorized access)
    private String panNumber;           // Full number (only for authorized access)
    private String passportNumber;
    private String drivingLicense;
    
    // Customer status
    private String customerType;        // INDIVIDUAL, CORPORATE, etc.
    private String customerStatus;      // ACTIVE, INACTIVE, CLOSED, etc.
    private String kycStatus;           // PENDING, COMPLETED, etc.
    private LocalDateTime kycCompletionDate;
    
    // Constructors
    public UserProfileResponse() {}
    
    // Getters and Setters
    
    // INSERT-ONLY paradigm fields
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getCustomerNumber() {
        return customerNumber;
    }
    
    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }
    
    public String getCrudOperation() {
        return crudOperation;
    }
    
    public void setCrudOperation(String crudOperation) {
        this.crudOperation = crudOperation;
    }
    
    public LocalDateTime getVersionTimestamp() {
        return versionTimestamp;
    }
    
    public void setVersionTimestamp(LocalDateTime versionTimestamp) {
        this.versionTimestamp = versionTimestamp;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
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
    
    public String getMaskedAadhar() {
        return maskedAadhar;
    }
    
    public void setMaskedAadhar(String maskedAadhar) {
        this.maskedAadhar = maskedAadhar;
    }
    
    public String getMaskedPan() {
        return maskedPan;
    }
    
    public void setMaskedPan(String maskedPan) {
        this.maskedPan = maskedPan;
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
    
    // Customer status fields
    public String getCustomerType() {
        return customerType;
    }
    
    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }
    
    public String getCustomerStatus() {
        return customerStatus;
    }
    
    public void setCustomerStatus(String customerStatus) {
        this.customerStatus = customerStatus;
    }
    
    public String getKycStatus() {
        return kycStatus;
    }
    
    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }
    
    public LocalDateTime getKycCompletionDate() {
        return kycCompletionDate;
    }
    
    public void setKycCompletionDate(LocalDateTime kycCompletionDate) {
        this.kycCompletionDate = kycCompletionDate;
    }
}
