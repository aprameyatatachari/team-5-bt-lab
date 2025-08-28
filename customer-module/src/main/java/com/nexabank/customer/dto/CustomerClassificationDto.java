package com.nexabank.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerClassificationDto {
    
    private String id;
    private String customerNumber;
    private String classificationType;
    private String classificationValue;
    private LocalDateTime effectiveDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
