package com.nexabank.customer.controller;

import com.nexabank.customer.dto.CreateUserProfileRequest;
import com.nexabank.customer.dto.UserProfileResponse;
import com.nexabank.customer.entity.Customer;
import com.nexabank.customer.entity.CustomerIdentification;
import com.nexabank.customer.entity.CustomerNameComponent;
import com.nexabank.customer.service.CustomerService;
import com.nexabank.customer.service.CustomerIdentificationService;
import com.nexabank.customer.service.CustomerNameComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Customer Profile Management", description = "CRUD operations for customer profile information")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private CustomerIdentificationService identificationService;
    
    @Autowired
    private CustomerNameComponentService nameComponentService;
    
    /**
     * Create new customer profile (called by auth-module during registration)
     */
    @PostMapping
    @Operation(
        summary = "Create new customer profile",
        description = "Creates a new customer profile with normalized data structure. This endpoint is typically called by the authentication module during user registration."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Customer profile created successfully", 
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data or customer already exists", 
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error", 
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createProfile(
        @Parameter(description = "Customer profile creation request", required = true)
        @RequestBody CreateUserProfileRequest request) {
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
            
            Customer savedCustomer = customerService.createCustomer(customer);
            
            // Create name component
            if (request.getFirstName() != null || request.getLastName() != null || request.getMiddleName() != null) {
                if (request.getFirstName() != null) {
                    CustomerNameComponent firstName = new CustomerNameComponent();
                    firstName.setCustomer(savedCustomer);
                    firstName.setNameComponentType(CustomerNameComponent.NameComponentType.FIRST_NAME);
                    firstName.setNameValue(request.getFirstName());
                    firstName.setEffectiveDate(LocalDateTime.now());
                    nameComponentService.save(firstName);
                }
                
                if (request.getLastName() != null) {
                    CustomerNameComponent lastName = new CustomerNameComponent();
                    lastName.setCustomer(savedCustomer);
                    lastName.setNameComponentType(CustomerNameComponent.NameComponentType.LAST_NAME);
                    lastName.setNameValue(request.getLastName());
                    lastName.setEffectiveDate(LocalDateTime.now());
                    nameComponentService.save(lastName);
                }
                
                if (request.getMiddleName() != null) {
                    CustomerNameComponent middleName = new CustomerNameComponent();
                    middleName.setCustomer(savedCustomer);
                    middleName.setNameComponentType(CustomerNameComponent.NameComponentType.MIDDLE_NAME);
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
            
            // Note: Passport and driving license now stored as CustomerIdentification
            if (request.getPassportNumber() != null) {
                CustomerIdentification passport = new CustomerIdentification();
                passport.setCustomer(savedCustomer);
                passport.setIdentificationType(CustomerIdentification.PASSPORT);
                passport.setIdentificationItem(request.getPassportNumber());
                passport.setEffectiveDate(LocalDateTime.now());
                identificationService.save(passport);
            }
            
            if (request.getDrivingLicense() != null) {
                CustomerIdentification license = new CustomerIdentification();
                license.setCustomer(savedCustomer);
                license.setIdentificationType(CustomerIdentification.DRIVING_LICENSE);
                license.setIdentificationItem(request.getDrivingLicense());
                license.setEffectiveDate(LocalDateTime.now());
                identificationService.save(license);
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
    @Operation(
        summary = "Get customer profile by user ID",
        description = "Retrieves customer profile information using the user ID from the authentication module. This is the primary endpoint for inter-module communication."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer profile found", 
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "404", description = "Customer profile not found", 
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error", 
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getProfileByUserId(
        @Parameter(description = "User ID from authentication module", required = true, example = "user123")
        @PathVariable String userId) {
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
            
            Customer savedCustomer = customerService.updateCustomer(customer);
            
            // Update name components
            // For simplicity, delete and recreate name components
            nameComponentService.deleteByCustomerCustomerId(customer.getCustomerId());
            
            if (request.getFirstName() != null) {
                CustomerNameComponent firstName = new CustomerNameComponent();
                firstName.setCustomer(savedCustomer);
                firstName.setNameComponentType(CustomerNameComponent.NameComponentType.FIRST_NAME);
                firstName.setNameValue(request.getFirstName());
                firstName.setEffectiveDate(LocalDateTime.now());
                nameComponentService.save(firstName);
            }
            
            if (request.getLastName() != null) {
                CustomerNameComponent lastName = new CustomerNameComponent();
                lastName.setCustomer(savedCustomer);
                lastName.setNameComponentType(CustomerNameComponent.NameComponentType.LAST_NAME);
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
            nameComponentService.deleteByCustomerCustomerId(customer.getCustomerId());
            identificationService.deleteByCustomerCustomerId(customer.getCustomerId());
            
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
        response.setProfileId(customer.getCustomerId());
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
        
        // Get name from normalized table using customer ID
        List<CustomerNameComponent> nameComponents = nameComponentService.findByCustomerCustomerId(customer.getCustomerId());
        for (CustomerNameComponent nameComponent : nameComponents) {
            if (CustomerNameComponent.NameComponentType.FIRST_NAME.equals(nameComponent.getNameComponentType())) {
                response.setFirstName(nameComponent.getNameValue());
            } else if (CustomerNameComponent.NameComponentType.LAST_NAME.equals(nameComponent.getNameComponentType())) {
                response.setLastName(nameComponent.getNameValue());
            } else if (CustomerNameComponent.NameComponentType.MIDDLE_NAME.equals(nameComponent.getNameComponentType())) {
                response.setMiddleName(nameComponent.getNameValue());
            }
        }
        
        // Get identification numbers from normalized table using customer ID
        List<CustomerIdentification> identifications = identificationService.findByCustomerCustomerId(customer.getCustomerId());
        for (CustomerIdentification id : identifications) {
            if (CustomerIdentification.AADHAR_CARD.equals(id.getIdentificationType())) {
                response.setAadharNumber(id.getIdentificationItem());
            } else if (CustomerIdentification.PAN_CARD.equals(id.getIdentificationType())) {
                response.setPanNumber(id.getIdentificationItem());
            } else if (CustomerIdentification.PASSPORT.equals(id.getIdentificationType())) {
                response.setPassportNumber(id.getIdentificationItem());
            } else if (CustomerIdentification.DRIVING_LICENSE.equals(id.getIdentificationType())) {
                response.setDrivingLicense(id.getIdentificationItem());
            }
        }
        
        return response;
    }
}