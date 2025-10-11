package com.nexabank.customer.controller;

import com.nexabank.customer.dto.CreateUserProfileRequest;
import com.nexabank.customer.dto.UserProfileResponse;
import com.nexabank.customer.entity.Customer;
import com.nexabank.customer.entity.CustomerIdentification;
import com.nexabank.customer.entity.CustomerNameComponent;
import com.nexabank.customer.entity.CustomerProofOfIdentity;
import com.nexabank.customer.service.CustomerService;
import com.nexabank.customer.service.CustomerIdentificationService;
import com.nexabank.customer.service.CustomerNameComponentService;
import com.nexabank.customer.service.CustomerProofOfIdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Normalized Customer Controller
 * Replaces UserProfile functionality with proper Customer entity and normalized tables
 */
@RestController
@RequestMapping("/api/profiles")
@CrossOrigin(origins = "*")
public class UserProfileController {
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private CustomerIdentificationService identificationService;
    
    @Autowired
    private CustomerNameComponentService nameComponentService;
    
    @Autowired
    private CustomerProofOfIdentityService proofOfIdentityService;
    
    /**
     * Create new customer profile (called by auth-module during registration)
     */
    @PostMapping
    public ResponseEntity<?> createProfile(@RequestBody CreateUserProfileRequest request) {
        try {
            // Check if customer already exists
            if (customerService.existsByUserId(request.getUserId())) {
                return ResponseEntity.badRequest().body("Customer profile already exists for user: " + request.getUserId());
            }
            
            // Create main customer record
            Customer customer = new Customer();
            customer.setUserId(request.getUserId());
            customer.setEmailId(request.getEmail()); // Note: using emailId field
            customer.setDateOfBirth(request.getDateOfBirth());
            customer.setGender(request.getGender());
            customer.setNationality(request.getNationality());
            customer.setPhoneNumber(request.getPhoneNumber());
            customer.setAlternatePhone(request.getAlternatePhone());
            customer.setAddressLine1(request.getAddressLine1());
            customer.setAddressLine2(request.getAddressLine2());
            customer.setCity(request.getCity());
            customer.setState(request.getState());
            customer.setCountry(request.getCountry());
            customer.setPostalCode(request.getPostalCode());
            customer.setOccupation(request.getOccupation());
            customer.setEmployerName(request.getEmployerName());
            customer.setAnnualIncome(request.getAnnualIncome());
            
            Customer savedCustomer = customerService.createCustomer(customer);
            
            // Create name component
            if (request.getFirstName() != null || request.getLastName() != null || request.getMiddleName() != null) {
                if (request.getFirstName() != null) {
                    CustomerNameComponent firstName = new CustomerNameComponent();
                    firstName.setCustomer(savedCustomer);
                    firstName.setNameComponentType(CustomerNameComponent.FIRST_NAME);
                    firstName.setNameValue(request.getFirstName());
                    firstName.setEffectiveDate(LocalDateTime.now());
                    nameComponentService.save(firstName);
                }
                
                if (request.getLastName() != null) {
                    CustomerNameComponent lastName = new CustomerNameComponent();
                    lastName.setCustomer(savedCustomer);
                    lastName.setNameComponentType(CustomerNameComponent.LAST_NAME);
                    lastName.setNameValue(request.getLastName());
                    lastName.setEffectiveDate(LocalDateTime.now());
                    nameComponentService.save(lastName);
                }
                
                if (request.getMiddleName() != null) {
                    CustomerNameComponent middleName = new CustomerNameComponent();
                    middleName.setCustomer(savedCustomer);
                    middleName.setNameComponentType(CustomerNameComponent.MIDDLE_NAME);
                    middleName.setNameValue(request.getMiddleName());
                    middleName.setEffectiveDate(LocalDateTime.now());
                    nameComponentService.save(middleName);
                }
            }
            
            // Create identification documents
            if (request.getAadharNumber() != null) {
                CustomerIdentification aadhar = new CustomerIdentification();
                aadhar.setCustomer(savedCustomer);
                aadhar.setIdentificationType(CustomerIdentification.AADHAR_CARD);
                aadhar.setIdentificationItem(request.getAadharNumber());
                aadhar.setEffectiveDate(LocalDateTime.now());
                identificationService.save(aadhar);
            }
            
            if (request.getPanNumber() != null) {
                CustomerIdentification pan = new CustomerIdentification();
                pan.setCustomer(savedCustomer);
                pan.setIdentificationType(CustomerIdentification.PAN_CARD);
                pan.setIdentificationItem(request.getPanNumber());
                pan.setEffectiveDate(LocalDateTime.now());
                identificationService.save(pan);
            }
            
            // Create proof of identity documents
            if (request.getPassportNumber() != null) {
                CustomerProofOfIdentity passport = new CustomerProofOfIdentity();
                passport.setCustomer(savedCustomer);
                passport.setProofOfIdType(CustomerProofOfIdentity.PASSPORT);
                passport.setClassificationTypeValue(request.getPassportNumber());
                passport.setEffectiveDate(LocalDateTime.now());
                proofOfIdentityService.save(passport);
            }
            
            if (request.getDrivingLicense() != null) {
                CustomerProofOfIdentity license = new CustomerProofOfIdentity();
                license.setCustomer(savedCustomer);
                license.setProofOfIdType(CustomerProofOfIdentity.DRIVING_LICENSE);
                license.setClassificationTypeValue(request.getDrivingLicense());
                license.setEffectiveDate(LocalDateTime.now());
                proofOfIdentityService.save(license);
            }
            
            // Create response using normalized data
            UserProfileResponse response = createUserProfileResponse(savedCustomer);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Get profile by userId (for other modules)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getProfileByUserId(@PathVariable String userId) {
        try {
            Customer customer = customerService.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Customer profile not found for userId: " + userId));
            UserProfileResponse response = createUserProfileResponse(customer);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    
    /**
     * Update profile by userId (for other modules)
     */
    @PutMapping("/user/{userId}")
    public ResponseEntity<?> updateProfileByUserId(
            @PathVariable String userId,
            @RequestBody CreateUserProfileRequest request) {
        try {
            Customer customer = customerService.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Customer profile not found for userId: " + userId));
            
            // Update customer fields
            customer.setEmailId(request.getEmail());
            customer.setDateOfBirth(request.getDateOfBirth());
            customer.setGender(request.getGender());
            customer.setNationality(request.getNationality());
            customer.setPhoneNumber(request.getPhoneNumber());
            customer.setAlternatePhone(request.getAlternatePhone());
            customer.setAddressLine1(request.getAddressLine1());
            customer.setAddressLine2(request.getAddressLine2());
            customer.setCity(request.getCity());
            customer.setState(request.getState());
            customer.setCountry(request.getCountry());
            customer.setPostalCode(request.getPostalCode());
            customer.setOccupation(request.getOccupation());
            customer.setEmployerName(request.getEmployerName());
            customer.setAnnualIncome(request.getAnnualIncome());
            
            Customer savedCustomer = customerService.updateCustomer(customer);
            
            // Update name components
            // For simplicity, delete and recreate name components
            nameComponentService.deleteByCustomerCustomerNumber(customer.getCustomerNumber());
            
            if (request.getFirstName() != null) {
                CustomerNameComponent firstName = new CustomerNameComponent();
                firstName.setCustomer(savedCustomer);
                firstName.setNameComponentType(CustomerNameComponent.FIRST_NAME);
                firstName.setNameValue(request.getFirstName());
                firstName.setEffectiveDate(LocalDateTime.now());
                nameComponentService.save(firstName);
            }
            
            if (request.getLastName() != null) {
                CustomerNameComponent lastName = new CustomerNameComponent();
                lastName.setCustomer(savedCustomer);
                lastName.setNameComponentType(CustomerNameComponent.LAST_NAME);
                lastName.setNameValue(request.getLastName());
                lastName.setEffectiveDate(LocalDateTime.now());
                nameComponentService.save(lastName);
            }
            
            UserProfileResponse response = createUserProfileResponse(savedCustomer);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Get all profiles (for admin modules)
     */
    @GetMapping
    public ResponseEntity<?> getAllProfiles() {
        try {
            List<Customer> customers = customerService.findAllCustomers();
            List<UserProfileResponse> responses = customers.stream()
                .map(this::createUserProfileResponse)
                .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    
    /**
     * Search profiles by name (for admin modules)
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchProfiles(@RequestParam String name) {
        try {
            List<Customer> customers = customerService.searchByName(name);
            List<UserProfileResponse> responses = customers.stream()
                .map(this::createUserProfileResponse)
                .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    
    /**
     * Delete profile by userId (for admin modules)
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> deleteProfile(@PathVariable String userId) {
        try {
            Customer customer = customerService.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Customer profile not found for userId: " + userId));
            
            // Delete related records first
            nameComponentService.deleteByCustomerCustomerNumber(customer.getCustomerNumber());
            identificationService.deleteByCustomerCustomerNumber(customer.getCustomerNumber());
            proofOfIdentityService.deleteProofOfIdentitiesByCustomerNumber(customer.getCustomerNumber());
            
            // Delete customer (soft delete)
            customerService.deleteCustomer(customer.getCustomerId());
            
            return ResponseEntity.ok("Customer profile deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    
    /**
     * Get profile by email (for other modules that need email lookup)
     */
    @GetMapping("/email/{email:.+}")
    public ResponseEntity<?> getProfileByEmail(@PathVariable String email) {
        try {
            Optional<Customer> customerOpt = customerService.findByEmail(email);
            if (customerOpt.isPresent()) {
                UserProfileResponse response = createUserProfileResponse(customerOpt.get());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer profile not found for email: " + email);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    
    /**
     * Helper method to create UserProfileResponse from normalized Customer data
     */
    private UserProfileResponse createUserProfileResponse(Customer customer) {
        UserProfileResponse response = new UserProfileResponse();
        
        // Basic customer data
        response.setProfileId(customer.getCustomerNumber());
        response.setUserId(customer.getUserId());
        response.setEmail(customer.getEmailId()); // Note: using emailId field
        response.setDateOfBirth(customer.getDateOfBirth());
        response.setGender(customer.getGender());
        response.setNationality(customer.getNationality());
        response.setPhoneNumber(customer.getPhoneNumber());
        response.setAlternatePhone(customer.getAlternatePhone());
        response.setAddressLine1(customer.getAddressLine1());
        response.setAddressLine2(customer.getAddressLine2());
        response.setCity(customer.getCity());
        response.setState(customer.getState());
        response.setCountry(customer.getCountry());
        response.setPostalCode(customer.getPostalCode());
        response.setOccupation(customer.getOccupation());
        response.setEmployerName(customer.getEmployerName());
        response.setAnnualIncome(customer.getAnnualIncome());
        
        // Get name from normalized table
        List<CustomerNameComponent> nameComponents = nameComponentService.findByCustomerCustomerNumber(customer.getCustomerNumber());
        for (CustomerNameComponent nameComponent : nameComponents) {
            if (CustomerNameComponent.FIRST_NAME.equals(nameComponent.getNameComponentType())) {
                response.setFirstName(nameComponent.getNameValue());
            } else if (CustomerNameComponent.LAST_NAME.equals(nameComponent.getNameComponentType())) {
                response.setLastName(nameComponent.getNameValue());
            } else if (CustomerNameComponent.MIDDLE_NAME.equals(nameComponent.getNameComponentType())) {
                response.setMiddleName(nameComponent.getNameValue());
            }
        }
        
        // Get identification numbers from normalized table
        List<CustomerIdentification> identifications = identificationService.findByCustomerCustomerNumber(customer.getCustomerNumber());
        for (CustomerIdentification id : identifications) {
            if (CustomerIdentification.AADHAR_CARD.equals(id.getIdentificationType())) {
                response.setAadharNumber(id.getIdentificationItem());
            } else if (CustomerIdentification.PAN_CARD.equals(id.getIdentificationType())) {
                response.setPanNumber(id.getIdentificationItem());
            }
        }
        
        // Get proof documents from normalized table
        List<CustomerProofOfIdentity> proofs = proofOfIdentityService.getProofOfIdentitiesByCustomerNumber(customer.getCustomerNumber())
            .stream().map(dto -> {
                CustomerProofOfIdentity entity = new CustomerProofOfIdentity();
                entity.setProofOfIdType(dto.getProofOfIdType());
                entity.setClassificationTypeValue(dto.getClassificationTypeValue());
                return entity;
            }).collect(Collectors.toList());
            
        for (CustomerProofOfIdentity proof : proofs) {
            if (CustomerProofOfIdentity.PASSPORT.equals(proof.getProofOfIdType())) {
                response.setPassportNumber(proof.getClassificationTypeValue());
            } else if (CustomerProofOfIdentity.DRIVING_LICENSE.equals(proof.getProofOfIdType())) {
                response.setDrivingLicense(proof.getClassificationTypeValue());
            }
        }
        
        return response;
    }
}