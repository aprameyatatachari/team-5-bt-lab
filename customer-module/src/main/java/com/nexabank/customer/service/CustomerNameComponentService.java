package com.nexabank.customer.service;

import com.nexabank.customer.dto.CustomerNameComponentDto;
import com.nexabank.customer.entity.CustomerNameComponent;
import com.nexabank.customer.mapper.CustomerMapper;
import com.nexabank.customer.repository.CustomerNameComponentRepository;
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
public class CustomerNameComponentService {
    
    private final CustomerNameComponentRepository nameComponentRepository;
    private final CustomerMapper customerMapper;
    
    public CustomerNameComponentDto createNameComponent(CustomerNameComponentDto nameComponentDto) {
        log.info("Creating name component for customer: {}", nameComponentDto.getCustomerNumber());
        
        CustomerNameComponent nameComponent = customerMapper.toNameComponentEntity(nameComponentDto);
        nameComponent.setEffectiveDate(LocalDateTime.now());
        
        CustomerNameComponent savedComponent = nameComponentRepository.save(nameComponent);
        log.info("Name component created with ID: {}", savedComponent.getId());
        
        return customerMapper.toNameComponentDto(savedComponent);
    }
    
    public List<CustomerNameComponentDto> getNameComponentsByCustomerNumber(String customerNumber) {
        log.info("Fetching name components for customer: {}", customerNumber);
        
        return nameComponentRepository.findByCustomerNumber(customerNumber)
                .stream()
                .map(customerMapper::toNameComponentDto)
                .collect(Collectors.toList());
    }
    
    public List<CustomerNameComponentDto> getNameComponentsByType(String customerNumber, String nameComponentType) {
        log.info("Fetching name components for customer: {} with type: {}", customerNumber, nameComponentType);
        
        return nameComponentRepository.findByCustomerNumberAndNameComponentType(customerNumber, nameComponentType)
                .stream()
                .map(customerMapper::toNameComponentDto)
                .collect(Collectors.toList());
    }
    
    public Optional<CustomerNameComponentDto> getNameComponentById(String id) {
        log.info("Fetching name component by ID: {}", id);
        
        return nameComponentRepository.findById(id)
                .map(customerMapper::toNameComponentDto);
    }
    
    public CustomerNameComponentDto updateNameComponent(String id, CustomerNameComponentDto nameComponentDto) {
        log.info("Updating name component: {}", id);
        
        CustomerNameComponent existingComponent = nameComponentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Name component not found: " + id));
        
        existingComponent.setNameComponentType(nameComponentDto.getNameComponentType());
        existingComponent.setNameValue(nameComponentDto.getNameValue());
        
        CustomerNameComponent updatedComponent = nameComponentRepository.save(existingComponent);
        log.info("Name component updated: {}", id);
        
        return customerMapper.toNameComponentDto(updatedComponent);
    }
    
    public void deleteNameComponent(String id) {
        log.info("Deleting name component: {}", id);
        
        CustomerNameComponent component = nameComponentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Name component not found: " + id));
        
        nameComponentRepository.delete(component);
        log.info("Name component deleted: {}", id);
    }
    
    public void deleteNameComponentsByCustomerNumber(String customerNumber) {
        log.info("Deleting all name components for customer: {}", customerNumber);
        
        List<CustomerNameComponent> components = nameComponentRepository.findByCustomerNumber(customerNumber);
        nameComponentRepository.deleteAll(components);
        log.info("All name components deleted for customer: {}", customerNumber);
    }
}
