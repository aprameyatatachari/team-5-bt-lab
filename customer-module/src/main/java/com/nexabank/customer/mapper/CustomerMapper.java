package com.nexabank.customer.mapper;

import com.nexabank.customer.dto.CustomerDetailsDto;
import com.nexabank.customer.dto.CustomerNameComponentDto;
import com.nexabank.customer.dto.CustomerProofOfIdentityDto;
import com.nexabank.customer.entity.CustomerDetails;
import com.nexabank.customer.entity.CustomerNameComponent;
import com.nexabank.customer.entity.CustomerProofOfIdentity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CustomerMapper {
    
    public CustomerDetailsDto toDto(CustomerDetails entity) {
        if (entity == null) return null;
        
        CustomerDetailsDto dto = new CustomerDetailsDto();
        dto.setCustomerNumber(entity.getCustomerNumber());
        dto.setCustomerType(entity.getCustomerType());
        dto.setCustomerFullName(entity.getCustomerFullName());
        dto.setCustomerDobDoi(entity.getCustomerDobDoi());
        dto.setCustomerStatus(entity.getCustomerStatus().toString());
        dto.setCustomerContactNumber(entity.getCustomerContactNumber());
        dto.setCustomerMobileNumber(entity.getCustomerMobileNumber());
        dto.setCustomerEmailId(entity.getCustomerEmailId());
        dto.setCustomerCountryOfOrigination(entity.getCustomerCountryOfOrigination());
        dto.setEffectiveDate(entity.getEffectiveDate());
        dto.setUserId(entity.getUserId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        // Map name components
        if (entity.getNameComponents() != null) {
            dto.setNameComponents(entity.getNameComponents().stream()
                .map(this::toNameComponentDto)
                .collect(Collectors.toList()));
        }
        
        // Map proof of identities
        if (entity.getProofOfIdentities() != null) {
            dto.setProofOfIdentities(entity.getProofOfIdentities().stream()
                .map(this::toProofOfIdentityDto)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    public CustomerDetails toEntity(CustomerDetailsDto dto) {
        if (dto == null) return null;
        
        CustomerDetails entity = new CustomerDetails();
        entity.setCustomerNumber(dto.getCustomerNumber());
        entity.setCustomerType(dto.getCustomerType());
        entity.setCustomerFullName(dto.getCustomerFullName());
        entity.setCustomerDobDoi(dto.getCustomerDobDoi());
        entity.setCustomerStatus(CustomerDetails.CustomerStatus.valueOf(dto.getCustomerStatus()));
        entity.setCustomerContactNumber(dto.getCustomerContactNumber());
        entity.setCustomerMobileNumber(dto.getCustomerMobileNumber());
        entity.setCustomerEmailId(dto.getCustomerEmailId());
        entity.setCustomerCountryOfOrigination(dto.getCustomerCountryOfOrigination());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setUserId(dto.getUserId());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        
        return entity;
    }
    
    public CustomerNameComponentDto toNameComponentDto(CustomerNameComponent entity) {
        if (entity == null) return null;
        
        CustomerNameComponentDto dto = new CustomerNameComponentDto();
        dto.setId(entity.getId());
        dto.setCustomerNumber(entity.getCustomerNumber());
        dto.setNameComponentType(entity.getNameComponentType());
        dto.setNameValue(entity.getNameValue());
        dto.setEffectiveDate(entity.getEffectiveDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        return dto;
    }
    
    public CustomerNameComponent toNameComponentEntity(CustomerNameComponentDto dto) {
        if (dto == null) return null;
        
        CustomerNameComponent entity = new CustomerNameComponent();
        entity.setId(dto.getId());
        entity.setCustomerNumber(dto.getCustomerNumber());
        entity.setNameComponentType(dto.getNameComponentType());
        entity.setNameValue(dto.getNameValue());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        
        return entity;
    }
    
    public CustomerProofOfIdentityDto toProofOfIdentityDto(CustomerProofOfIdentity entity) {
        if (entity == null) return null;
        
        CustomerProofOfIdentityDto dto = new CustomerProofOfIdentityDto();
        dto.setId(entity.getId());
        dto.setCustomerNumber(entity.getCustomerNumber());
        dto.setProofOfIdType(entity.getProofOfIdType());
        dto.setClassificationTypeValue(entity.getClassificationTypeValue());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setEffectiveDate(entity.getEffectiveDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        return dto;
    }
    
    public CustomerProofOfIdentity toProofOfIdentityEntity(CustomerProofOfIdentityDto dto) {
        if (dto == null) return null;
        
        CustomerProofOfIdentity entity = new CustomerProofOfIdentity();
        entity.setId(dto.getId());
        entity.setCustomerNumber(dto.getCustomerNumber());
        entity.setProofOfIdType(dto.getProofOfIdType());
        entity.setClassificationTypeValue(dto.getClassificationTypeValue());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        
        return entity;
    }
}
