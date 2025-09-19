package com.nexabank.customer.service;

import com.nexabank.customer.dto.CustomerProofOfIdentityDto;
import com.nexabank.customer.entity.CustomerProofOfIdentity;
import com.nexabank.customer.repository.CustomerProofOfIdentityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerProofOfIdentityService {

    private final CustomerProofOfIdentityRepository proofOfIdentityRepository;

    /**
     * Create a new proof of identity
     */
    public CustomerProofOfIdentityDto createProofOfIdentity(CustomerProofOfIdentityDto dto) {
        log.info("Creating proof of identity for customer: {}", dto.getCustomerNumber());
        
        CustomerProofOfIdentity entity = convertToEntity(dto);
        CustomerProofOfIdentity savedEntity = proofOfIdentityRepository.save(entity);
        
        log.info("Proof of identity created with ID: {}", savedEntity.getId());
        return convertToDto(savedEntity);
    }

    /**
     * Get all proof of identity records for a customer
     */
    @Transactional(readOnly = true)
    public List<CustomerProofOfIdentityDto> getProofOfIdentitiesByCustomerNumber(String customerNumber) {
        log.info("Fetching proof of identities for customer: {}", customerNumber);
        
        List<CustomerProofOfIdentity> entities = proofOfIdentityRepository.findByCustomerNumber(customerNumber);
        return entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get proof of identity by ID
     */
    @Transactional(readOnly = true)
    public Optional<CustomerProofOfIdentityDto> getProofOfIdentityById(String id) {
        log.info("Fetching proof of identity by ID: {}", id);
        
        return proofOfIdentityRepository.findById(id)
                .map(this::convertToDto);
    }

    /**
     * Update proof of identity
     */
    public CustomerProofOfIdentityDto updateProofOfIdentity(String id, CustomerProofOfIdentityDto dto) {
        log.info("Updating proof of identity: {}", id);
        
        CustomerProofOfIdentity existingEntity = proofOfIdentityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proof of identity not found: " + id));
        
        // Update fields
        existingEntity.setProofOfIdType(dto.getProofOfIdType());
        existingEntity.setClassificationTypeValue(dto.getClassificationTypeValue());
        existingEntity.setStartDate(dto.getStartDate());
        existingEntity.setEndDate(dto.getEndDate());
        existingEntity.setEffectiveDate(dto.getEffectiveDate());
        
        CustomerProofOfIdentity updatedEntity = proofOfIdentityRepository.save(existingEntity);
        
        log.info("Proof of identity updated: {}", id);
        return convertToDto(updatedEntity);
    }

    /**
     * Delete proof of identity
     */
    public void deleteProofOfIdentity(String id) {
        log.info("Deleting proof of identity: {}", id);
        
        if (!proofOfIdentityRepository.existsById(id)) {
            throw new RuntimeException("Proof of identity not found: " + id);
        }
        
        proofOfIdentityRepository.deleteById(id);
        log.info("Proof of identity deleted: {}", id);
    }

    /**
     * Delete all proof of identity records for a customer
     */
    public void deleteProofOfIdentitiesByCustomerNumber(String customerNumber) {
        log.info("Deleting all proof of identities for customer: {}", customerNumber);
        
        proofOfIdentityRepository.deleteByCustomerNumber(customerNumber);
        log.info("All proof of identities deleted for customer: {}", customerNumber);
    }

    /**
     * Get proof of identity by customer number and type
     */
    @Transactional(readOnly = true)
    public Optional<CustomerProofOfIdentityDto> getProofOfIdentityByCustomerNumberAndType(String customerNumber, String proofType) {
        log.info("Fetching proof of identity for customer: {} and type: {}", customerNumber, proofType);
        
        return proofOfIdentityRepository.findByCustomerNumberAndProofOfIdType(customerNumber, proofType)
                .map(this::convertToDto);
    }

    // Helper methods for conversion
    private CustomerProofOfIdentity convertToEntity(CustomerProofOfIdentityDto dto) {
        CustomerProofOfIdentity entity = new CustomerProofOfIdentity();
        entity.setCustomerNumber(dto.getCustomerNumber());
        entity.setProofOfIdType(dto.getProofOfIdType());
        entity.setClassificationTypeValue(dto.getClassificationTypeValue());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setEffectiveDate(dto.getEffectiveDate() != null ? dto.getEffectiveDate() : LocalDateTime.now());
        return entity;
    }

    private CustomerProofOfIdentityDto convertToDto(CustomerProofOfIdentity entity) {
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
}
