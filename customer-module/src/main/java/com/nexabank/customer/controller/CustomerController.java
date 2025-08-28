package com.nexabank.customer.controller;

import com.nexabank.customer.dto.CustomerDetailsDto;
import com.nexabank.customer.dto.CustomerNameComponentDto;
import com.nexabank.customer.service.CustomerDetailsService;
import com.nexabank.customer.service.CustomerNameComponentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CustomerController {
    
    private final CustomerDetailsService customerDetailsService;
    private final CustomerNameComponentService nameComponentService;
    
    @PostMapping
    public ResponseEntity<CustomerDetailsDto> createCustomer(@Valid @RequestBody CustomerDetailsDto customerDto) {
        log.info("Creating new customer: {}", customerDto.getCustomerFullName());
        try {
            CustomerDetailsDto createdCustomer = customerDetailsService.createCustomer(customerDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCustomer);
        } catch (Exception e) {
            log.error("Error creating customer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{customerNumber}")
    public ResponseEntity<CustomerDetailsDto> getCustomerByNumber(@PathVariable String customerNumber) {
        log.info("Fetching customer by number: {}", customerNumber);
        return customerDetailsService.getCustomerByNumber(customerNumber)
                .map(customer -> ResponseEntity.ok(customer))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<CustomerDetailsDto> getCustomerByEmail(@PathVariable String email) {
        log.info("Fetching customer by email: {}", email);
        return customerDetailsService.getCustomerByEmail(email)
                .map(customer -> ResponseEntity.ok(customer))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<CustomerDetailsDto> getCustomerByUserId(@PathVariable String userId) {
        log.info("Fetching customer by userId: {}", userId);
        return customerDetailsService.getCustomerByUserId(userId)
                .map(customer -> ResponseEntity.ok(customer))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<CustomerDetailsDto>> getAllCustomers() {
        log.info("Fetching all customers");
        List<CustomerDetailsDto> customers = customerDetailsService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<CustomerDetailsDto>> getCustomersByStatus(@PathVariable String status) {
        log.info("Fetching customers by status: {}", status);
        try {
            List<CustomerDetailsDto> customers = customerDetailsService.getCustomersByStatus(status);
            return ResponseEntity.ok(customers);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/type/{type}")
    public ResponseEntity<List<CustomerDetailsDto>> getCustomersByType(@PathVariable String type) {
        log.info("Fetching customers by type: {}", type);
        List<CustomerDetailsDto> customers = customerDetailsService.getCustomersByType(type);
        return ResponseEntity.ok(customers);
    }
    
    @PutMapping("/{customerNumber}")
    public ResponseEntity<CustomerDetailsDto> updateCustomer(
            @PathVariable String customerNumber,
            @Valid @RequestBody CustomerDetailsDto customerDto) {
        log.info("Updating customer: {}", customerNumber);
        try {
            CustomerDetailsDto updatedCustomer = customerDetailsService.updateCustomer(customerNumber, customerDto);
            return ResponseEntity.ok(updatedCustomer);
        } catch (RuntimeException e) {
            log.error("Customer not found: {}", customerNumber);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating customer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/{customerNumber}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String customerNumber) {
        log.info("Deleting customer: {}", customerNumber);
        try {
            customerDetailsService.deleteCustomer(customerNumber);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Customer not found: {}", customerNumber);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting customer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Name Component endpoints
    @PostMapping("/{customerNumber}/name-components")
    public ResponseEntity<CustomerNameComponentDto> createNameComponent(
            @PathVariable String customerNumber,
            @Valid @RequestBody CustomerNameComponentDto nameComponentDto) {
        log.info("Creating name component for customer: {}", customerNumber);
        try {
            nameComponentDto.setCustomerNumber(customerNumber);
            CustomerNameComponentDto createdComponent = nameComponentService.createNameComponent(nameComponentDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdComponent);
        } catch (Exception e) {
            log.error("Error creating name component: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{customerNumber}/name-components")
    public ResponseEntity<List<CustomerNameComponentDto>> getNameComponents(@PathVariable String customerNumber) {
        log.info("Fetching name components for customer: {}", customerNumber);
        List<CustomerNameComponentDto> components = nameComponentService.getNameComponentsByCustomerNumber(customerNumber);
        return ResponseEntity.ok(components);
    }
    
    @GetMapping("/{customerNumber}/name-components/type/{type}")
    public ResponseEntity<List<CustomerNameComponentDto>> getNameComponentsByType(
            @PathVariable String customerNumber,
            @PathVariable String type) {
        log.info("Fetching name components for customer: {} with type: {}", customerNumber, type);
        List<CustomerNameComponentDto> components = nameComponentService.getNameComponentsByType(customerNumber, type);
        return ResponseEntity.ok(components);
    }
    
    @PutMapping("/name-components/{componentId}")
    public ResponseEntity<CustomerNameComponentDto> updateNameComponent(
            @PathVariable String componentId,
            @Valid @RequestBody CustomerNameComponentDto nameComponentDto) {
        log.info("Updating name component: {}", componentId);
        try {
            CustomerNameComponentDto updatedComponent = nameComponentService.updateNameComponent(componentId, nameComponentDto);
            return ResponseEntity.ok(updatedComponent);
        } catch (RuntimeException e) {
            log.error("Name component not found: {}", componentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating name component: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/name-components/{componentId}")
    public ResponseEntity<Void> deleteNameComponent(@PathVariable String componentId) {
        log.info("Deleting name component: {}", componentId);
        try {
            nameComponentService.deleteNameComponent(componentId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Name component not found: {}", componentId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting name component: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Customer service is running");
    }
}
