package com.nexabank.customer.dto;

/**
 * DTO for updating customer identification documents
 */
public class UpdateIdentificationRequest {
    
    private String aadharNumber;
    private String panNumber;
    private String passportNumber;
    private String drivingLicense;
    
    // Constructors
    public UpdateIdentificationRequest() {}
    
    // Getters and Setters
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
}
