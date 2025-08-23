package com.login_module.dto;

import com.login_module.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    
    private LocalDate dateOfBirth;
    
    private String address;
    
    private String city;
    
    private String state;
    
    private String country = "India";
    
    private String postalCode;
    
    private String aadharNumber;
    
    private String panNumber;
    
    private User.UserType userType = User.UserType.CUSTOMER;
}
