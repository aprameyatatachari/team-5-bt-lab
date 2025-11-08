package com.nexabank.customer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Event DTO published to Kafka when a customer account is opened
 * This event is sent to the 'alert' topic for downstream processing
 * (e.g., welcome email, SMS notification, analytics)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAccountOpenedEvent {
    
    /**
     * Event metadata
     */
    private String eventId;
    private String eventType;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eventTimestamp;
    
    /**
     * Customer identification
     */
    private String customerNumber;
    private String userId;
    
    /**
     * Primary contact information (for alerts)
     */
    private String email;
    private String phoneNumber;
    private String alternatePhone;
    
    /**
     * Customer personal details
     */
    private String firstName;
    private String lastName;
    private String middleName;
    private String fullName;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    
    private String gender;
    private String nationality;
    
    /**
     * Address information
     */
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    
    /**
     * Professional information
     */
    private String occupation;
    private String employerName;
    private Double annualIncome;
    
    /**
     * Account metadata
     */
    private String source; // e.g., "WEB", "MOBILE_APP", "BRANCH"
    private String remarks;
}
