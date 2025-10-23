package com.nexabank.customer.dto;

/**
 * DTO for updating customer name
 */
public class UpdateNameRequest {
    
    private String firstName;
    private String middleName;
    private String lastName;
    
    // Constructors
    public UpdateNameRequest() {}
    
    // Getters and Setters
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getMiddleName() {
        return middleName;
    }
    
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
