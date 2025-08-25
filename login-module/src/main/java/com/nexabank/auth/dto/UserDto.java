package com.nexabank.auth.dto;

import com.nexabank.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private User.UserType userType;
    private User.UserStatus status;
    private LocalDateTime lastLogin;
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String maskedAadhar;
    private String maskedPan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructor from User entity
    public UserDto(User user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phoneNumber = user.getPhoneNumber();
        this.userType = user.getUserType();
        this.status = user.getStatus();
        this.lastLogin = user.getLastLogin();
        this.dateOfBirth = user.getDateOfBirth();
        this.address = user.getAddress();
        this.city = user.getCity();
        this.state = user.getState();
        this.country = user.getCountry();
        this.postalCode = user.getPostalCode();
        this.maskedAadhar = maskAadhar(user.getAadharNumber());
        this.maskedPan = maskPan(user.getPanNumber());
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
    
    private String maskAadhar(String aadhar) {
        if (aadhar == null || aadhar.length() < 4) {
            return aadhar;
        }
        return "*".repeat(aadhar.length() - 4) + aadhar.substring(aadhar.length() - 4);
    }
    
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 4) {
            return pan;
        }
        return "*".repeat(pan.length() - 4) + pan.substring(pan.length() - 4);
    }
}
