package com.nexabank.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDetailsDto {
    
    private String customerNumber;
    private String customerType;
    private String customerFullName;
    private LocalDate customerDobDoi;
    private String customerStatus;
    private String customerContactNumber;
    private String customerMobileNumber;
    private String customerEmailId;
    private String customerCountryOfOrigination;
    private LocalDateTime effectiveDate;
    private String userId;
    
    // Name components
    private List<CustomerNameComponentDto> nameComponents;
    
    // Proof of identities
    private List<CustomerProofOfIdentityDto> proofOfIdentities;
    
    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
