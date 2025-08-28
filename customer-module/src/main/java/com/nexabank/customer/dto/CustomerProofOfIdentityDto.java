package com.nexabank.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProofOfIdentityDto {
    
    private String id;
    private String customerNumber;
    private String proofOfIdType;
    private String classificationTypeValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime effectiveDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
