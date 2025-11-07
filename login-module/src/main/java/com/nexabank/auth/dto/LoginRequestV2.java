package com.nexabank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request DTO for v2 API
 * Uses customerNumber instead of email for authentication
 */
public class LoginRequestV2 {

    @NotBlank(message = "Customer number is required")
    private String customerNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private boolean rememberMe = false;

    public LoginRequestV2() {}

    public LoginRequestV2(String customerNumber, String password, boolean rememberMe) {
        this.customerNumber = customerNumber;
        this.password = password;
        this.rememberMe = rememberMe;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
