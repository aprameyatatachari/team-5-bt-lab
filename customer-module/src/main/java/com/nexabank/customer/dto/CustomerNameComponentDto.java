package com.nexabank.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerNameComponentDto {
    
    private String id;
    private String customerNumber;
    private String nameComponentType;
    private String nameValue;
    private LocalDateTime effectiveDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
