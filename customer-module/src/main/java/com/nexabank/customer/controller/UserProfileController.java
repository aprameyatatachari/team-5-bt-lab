package com.nexabank.customer.controller;

import com.nexabank.customer.dto.CreateUserProfileRequest;
import com.nexabank.customer.dto.UpdateAddressRequest;
import com.nexabank.customer.dto.UpdateNameRequest;
import com.nexabank.customer.dto.UpdateIdentificationRequest;
import com.nexabank.customer.dto.UserProfileResponse;
import com.nexabank.customer.entity.Customer;
import com.nexabank.customer.entity.CustomerIdentification;
import com.nexabank.customer.entity.CustomerNameComponent;
import com.nexabank.customer.entity.CustomerAddressComponent;
import com.nexabank.customer.service.CustomerService;
import com.nexabank.customer.service.CustomerIdentificationService;
import com.nexabank.customer.service.CustomerNameComponentService;
import com.nexabank.customer.service.CustomerAddressComponentService;
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

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    
    @Autowired
    private CustomerAddressComponentService addressComponentService;
    
    /**
     * Authorization helper method
     * Checks if the requesting user (from JWT) can access the requested userId's data
     * 
     * @param request HttpServletRequest containing userId and userType from JWT filter
     * @param targetUserId The userId being accessed in the API endpoint
     * @return true if access is allowed (admin or own data), false otherwise
     */
    private boolean isAuthorized(HttpServletRequest request, String targetUserId) {
        String userType = (String) request.getAttribute("userType");
        String requestingUserId = (String) request.getAttribute("userId");
        
        // Admins can access all data
        if ("ADMIN".equals(userType)) {
            return true;
        }
        
        // Customers can only access their own data
        if ("CUSTOMER".equals(userType) && requestingUserId != null && requestingUserId.equals(targetUserId)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if user is admin (used for admin-only endpoints like getAll, search, delete)
     */
    private boolean isAdmin(HttpServletRequest request) {
        String userType = (String) request.getAttribute("userType");
        return "ADMIN".equals(userType);
    }
    
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
            
            // Parse date of birth from String to LocalDate
            if (request.getDateOfBirth() != null && !request.getDateOfBirth().isEmpty()) {
                try {
                    customer.setDateOfBirth(LocalDate.parse(request.getDateOfBirth(), DateTimeFormatter.ISO_LOCAL_DATE));
                } catch (Exception e) {
                    System.err.println("Failed to parse date of birth: " + request.getDateOfBirth());
                }
            }
            
            customer.setGender(request.getGender());
            customer.setNationality(request.getNationality());
            customer.setPhoneNumber(request.getPhoneNumber());
            customer.setAlternatePhone(request.getAlternatePhone());
            
            // Handle both 'address' and 'addressLine1' fields for compatibility
            String primaryAddress = request.getAddressLine1() != null ? request.getAddressLine1() : request.getAddress();
            customer.setAddressLine1(primaryAddress);
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
                    firstName.setCustomerNumber(savedCustomer.getCustomerNumber());
                    nameComponentService.save(firstName);
                }
                
                if (request.getLastName() != null) {
                    CustomerNameComponent lastName = new CustomerNameComponent();
                    lastName.setCustomer(savedCustomer);
                    lastName.setNameComponentType(CustomerNameComponent.NameComponentType.LAST_NAME);
                    lastName.setNameValue(request.getLastName());
                    lastName.setEffectiveDate(LocalDateTime.now());
                    lastName.setCustomerNumber(savedCustomer.getCustomerNumber());
                    nameComponentService.save(lastName);
                }
                
                if (request.getMiddleName() != null) {
                    CustomerNameComponent middleName = new CustomerNameComponent();
                    middleName.setCustomer(savedCustomer);
                    middleName.setNameComponentType(CustomerNameComponent.NameComponentType.MIDDLE_NAME);
                    middleName.setNameValue(request.getMiddleName());
                    middleName.setEffectiveDate(LocalDateTime.now());
                    middleName.setCustomerNumber(savedCustomer.getCustomerNumber());
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
                aadhar.setCustomerNumber(savedCustomer.getCustomerNumber());
                identificationService.save(aadhar);
            }
            
            if (request.getPanNumber() != null) {
                CustomerIdentification pan = new CustomerIdentification();
                pan.setCustomer(savedCustomer);
                pan.setIdentificationType(CustomerIdentification.PAN_CARD);
                pan.setIdentificationItem(request.getPanNumber());
                pan.setEffectiveDate(LocalDateTime.now());
                pan.setCustomerNumber(savedCustomer.getCustomerNumber());
                identificationService.save(pan);
            }
            
            // Note: Passport and driving license now stored as CustomerIdentification
            if (request.getPassportNumber() != null) {
                CustomerIdentification passport = new CustomerIdentification();
                passport.setCustomer(savedCustomer);
                passport.setIdentificationType(CustomerIdentification.PASSPORT);
                passport.setIdentificationItem(request.getPassportNumber());
                passport.setEffectiveDate(LocalDateTime.now());
                passport.setCustomerNumber(savedCustomer.getCustomerNumber());
                identificationService.save(passport);
            }
            
            if (request.getDrivingLicense() != null) {
                CustomerIdentification license = new CustomerIdentification();
                license.setCustomer(savedCustomer);
                license.setIdentificationType(CustomerIdentification.DRIVING_LICENSE);
                license.setIdentificationItem(request.getDrivingLicense());
                license.setEffectiveDate(LocalDateTime.now());
                license.setCustomerNumber(savedCustomer.getCustomerNumber());
                identificationService.save(license);
            }
            
            // Create address components
            if (primaryAddress != null) {
                CustomerAddressComponent addressLine1Component = new CustomerAddressComponent();
                addressLine1Component.setCustomer(savedCustomer);
                addressLine1Component.setAddressComponentType(CustomerAddressComponent.AddressComponentType.ADDRESS_LINE_1);
                addressLine1Component.setAddressValue(primaryAddress);
                addressLine1Component.setEffectiveDate(LocalDateTime.now());
                addressLine1Component.setCustomerNumber(savedCustomer.getCustomerNumber());
                addressComponentService.save(addressLine1Component);
            }
            
            if (request.getAddressLine2() != null) {
                CustomerAddressComponent addressLine2Component = new CustomerAddressComponent();
                addressLine2Component.setCustomer(savedCustomer);
                addressLine2Component.setAddressComponentType(CustomerAddressComponent.AddressComponentType.ADDRESS_LINE_2);
                addressLine2Component.setAddressValue(request.getAddressLine2());
                addressLine2Component.setEffectiveDate(LocalDateTime.now());
                addressLine2Component.setCustomerNumber(savedCustomer.getCustomerNumber());
                addressComponentService.save(addressLine2Component);
            }
            
            if (request.getCity() != null) {
                CustomerAddressComponent cityComponent = new CustomerAddressComponent();
                cityComponent.setCustomer(savedCustomer);
                cityComponent.setAddressComponentType(CustomerAddressComponent.AddressComponentType.CITY);
                cityComponent.setAddressValue(request.getCity());
                cityComponent.setEffectiveDate(LocalDateTime.now());
                cityComponent.setCustomerNumber(savedCustomer.getCustomerNumber());
                addressComponentService.save(cityComponent);
            }
            
            if (request.getState() != null) {
                CustomerAddressComponent stateComponent = new CustomerAddressComponent();
                stateComponent.setCustomer(savedCustomer);
                stateComponent.setAddressComponentType(CustomerAddressComponent.AddressComponentType.STATE);
                stateComponent.setAddressValue(request.getState());
                stateComponent.setEffectiveDate(LocalDateTime.now());
                stateComponent.setCustomerNumber(savedCustomer.getCustomerNumber());
                addressComponentService.save(stateComponent);
            }
            
            if (request.getCountry() != null) {
                CustomerAddressComponent countryComponent = new CustomerAddressComponent();
                countryComponent.setCustomer(savedCustomer);
                countryComponent.setAddressComponentType(CustomerAddressComponent.AddressComponentType.COUNTRY);
                countryComponent.setAddressValue(request.getCountry());
                countryComponent.setEffectiveDate(LocalDateTime.now());
                countryComponent.setCustomerNumber(savedCustomer.getCustomerNumber());
                addressComponentService.save(countryComponent);
            }
            
            if (request.getPostalCode() != null) {
                CustomerAddressComponent postalCodeComponent = new CustomerAddressComponent();
                postalCodeComponent.setCustomer(savedCustomer);
                postalCodeComponent.setAddressComponentType(CustomerAddressComponent.AddressComponentType.POSTAL_CODE);
                postalCodeComponent.setAddressValue(request.getPostalCode());
                postalCodeComponent.setEffectiveDate(LocalDateTime.now());
                postalCodeComponent.setCustomerNumber(savedCustomer.getCustomerNumber());
                addressComponentService.save(postalCodeComponent);
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
        @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions", 
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Customer profile not found", 
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error", 
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getProfileByUserId(
        @Parameter(description = "User ID from authentication module", required = true, example = "user123")
        @PathVariable String userId,
        HttpServletRequest request) {
        try {
            // Authorization check: customers can only access their own data, admins can access all
            if (!isAuthorized(request, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: You can only access your own profile");
            }
            
            Customer customer = customerService.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Customer profile not found for userId: " + userId));
            UserProfileResponse response = createUserProfileResponse(customer);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    
    /**
     * Update profile by customer number
     */
    @PutMapping("/customer/{customerNumber}")
    @Operation(
        summary = "Update customer profile by customer number",
        description = "Updates customer profile information. Customers can only update their own profile, admins can update any profile."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Customer profile updated successfully",
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = UserProfileResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n" +
                            "  \"customerId\": \"550e8400-e29b-41d4-a716-446655440000\",\n" +
                            "  \"customerNumber\": \"CUST-20251024-000001\",\n" +
                            "  \"crudOperation\": \"U\",\n" +
                            "  \"versionTimestamp\": \"2025-01-24T10:30:00\",\n" +
                            "  \"userId\": \"user123\",\n" +
                            "  \"email\": \"updated.email@example.com\",\n" +
                            "  \"firstName\": \"John\",\n" +
                            "  \"lastName\": \"Doe\",\n" +
                            "  \"phoneNumber\": \"+1234567890\",\n" +
                            "  \"dateOfBirth\": \"1990-01-15\",\n" +
                            "  \"gender\": \"Male\",\n" +
                            "  \"nationality\": \"Indian\",\n" +
                            "  \"addressLine1\": \"123 Main Street\",\n" +
                            "  \"city\": \"Mumbai\",\n" +
                            "  \"state\": \"Maharashtra\",\n" +
                            "  \"country\": \"India\",\n" +
                            "  \"postalCode\": \"400001\",\n" +
                            "  \"customerStatus\": \"ACTIVE\",\n" +
                            "  \"kycStatus\": \"COMPLETED\"\n" +
                            "}"
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Customer profile not found")
    })
    public ResponseEntity<?> updateProfileByCustomerNumber(
            @Parameter(description = "Customer number (e.g., CUST-20251024-000001)", required = true, example = "CUST-20251024-000001")
            @PathVariable String customerNumber,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated profile information (Note: userId comes from JWT token, identification documents should be updated via /identification endpoint)",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = CreateUserProfileRequest.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "Update Profile Example",
                        summary = "Example request to update customer profile",
                        description = "Only include fields you want to update. userId is read from JWT token, not from request body. Identification documents (aadhar, pan, etc.) should be updated via the /identification endpoint.",
                        value = "{\n" +
                                "  \"email\": \"updated.email@example.com\",\n" +
                                "  \"firstName\": \"John\",\n" +
                                "  \"lastName\": \"Doe\",\n" +
                                "  \"middleName\": \"Michael\",\n" +
                                "  \"dateOfBirth\": \"1990-01-15\",\n" +
                                "  \"gender\": \"Male\",\n" +
                                "  \"nationality\": \"Indian\",\n" +
                                "  \"phoneNumber\": \"+919876543210\",\n" +
                                "  \"alternatePhone\": \"+919876543211\",\n" +
                                "  \"addressLine1\": \"123 Main Street\",\n" +
                                "  \"addressLine2\": \"Apartment 4B\",\n" +
                                "  \"city\": \"Mumbai\",\n" +
                                "  \"state\": \"Maharashtra\",\n" +
                                "  \"country\": \"India\",\n" +
                                "  \"postalCode\": \"400001\"\n" +
                                "}"
                    )
                )
            )
            @RequestBody CreateUserProfileRequest request,
            HttpServletRequest httpRequest) {
        try {
            // Find customer by customer number first
            Customer customer = customerService.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new RuntimeException("Customer profile not found for customerNumber: " + customerNumber));
            
            // Authorization check: customers can only update their own data, admins can access all
            if (!isAuthorized(httpRequest, customer.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: You can only update your own profile");
            }
            
            // Update customer fields
            customer.setEmailId(request.getEmail());
            
            // Parse date of birth from String to LocalDate
            if (request.getDateOfBirth() != null && !request.getDateOfBirth().isEmpty()) {
                try {
                    customer.setDateOfBirth(LocalDate.parse(request.getDateOfBirth(), DateTimeFormatter.ISO_LOCAL_DATE));
                } catch (Exception e) {
                    System.err.println("Failed to parse date of birth: " + request.getDateOfBirth());
                }
            }
            
            customer.setGender(request.getGender());
            customer.setNationality(request.getNationality());
            customer.setPhoneNumber(request.getPhoneNumber());
            customer.setAlternatePhone(request.getAlternatePhone());
            
            // Handle both 'address' and 'addressLine1' fields for compatibility
            String primaryAddress = request.getAddressLine1() != null ? request.getAddressLine1() : request.getAddress();
            customer.setAddressLine1(primaryAddress);
            customer.setAddressLine2(request.getAddressLine2());
            customer.setCity(request.getCity());
            customer.setState(request.getState());
            customer.setCountry(request.getCountry());
            customer.setPostalCode(request.getPostalCode());
            
            Customer savedCustomer = customerService.updateCustomer(customer);
            
            // Update name components
            // Create new name component versions for the updated customer
            
            if (request.getFirstName() != null) {
                CustomerNameComponent firstName = new CustomerNameComponent();
                firstName.setCustomer(savedCustomer);
                firstName.setNameComponentType(CustomerNameComponent.NameComponentType.FIRST_NAME);
                firstName.setNameValue(request.getFirstName());
                firstName.setEffectiveDate(LocalDateTime.now());
                firstName.setCrudOperation(CustomerNameComponent.CrudOperation.U);
                firstName.setVersionTimestamp(LocalDateTime.now());
                firstName.setCustomerNumber(savedCustomer.getCustomerNumber());
                nameComponentService.save(firstName);
            }
            
            if (request.getLastName() != null) {
                CustomerNameComponent lastName = new CustomerNameComponent();
                lastName.setCustomer(savedCustomer);
                lastName.setNameComponentType(CustomerNameComponent.NameComponentType.LAST_NAME);
                lastName.setNameValue(request.getLastName());
                lastName.setEffectiveDate(LocalDateTime.now());
                lastName.setCrudOperation(CustomerNameComponent.CrudOperation.U);
                lastName.setVersionTimestamp(LocalDateTime.now());
                lastName.setCustomerNumber(savedCustomer.getCustomerNumber());
                nameComponentService.save(lastName);
            }
            
            UserProfileResponse response = createUserProfileResponse(savedCustomer);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Get profile by customer number (for internal systems)
     */
    @GetMapping("/customer/{customerNumber}")
    @Operation(
        summary = "Get customer profile by customer number",
        description = "Retrieves latest non-deleted customer profile using the business identifier (customer number). This is the recommended endpoint for looking up customers by their business ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer profile found", 
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "404", description = "Customer profile not found", 
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getProfileByCustomerNumber(
        @Parameter(description = "Customer number (e.g., CUST-20251023-000001)", required = true)
        @PathVariable String customerNumber) {
        try {
            Customer customer = customerService.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new RuntimeException("Customer not found with customerNumber: " + customerNumber));
            UserProfileResponse response = createUserProfileResponse(customer);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    
    /**
     * Get all profiles (for admin modules)
     */
    @GetMapping
    public ResponseEntity<?> getAllProfiles(HttpServletRequest request) {
        try {
            // Admin-only endpoint
            if (!isAdmin(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: Admin privileges required");
            }
            
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
    public ResponseEntity<?> searchProfiles(@RequestParam String name, HttpServletRequest request) {
        try {
            // Admin-only endpoint
            if (!isAdmin(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: Admin privileges required");
            }
            
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
    public ResponseEntity<?> deleteProfile(@PathVariable String userId, HttpServletRequest request) {
        try {
            // Admin-only endpoint
            if (!isAdmin(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: Admin privileges required");
            }
            
            Customer customer = customerService.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Customer profile not found for userId: " + userId));
            
            // Delete related records first (using customerNumber for stable reference)
            nameComponentService.deleteByCustomerNumber(customer.getCustomerNumber());
            identificationService.deleteByCustomerNumber(customer.getCustomerNumber());
            addressComponentService.deleteByCustomerNumber(customer.getCustomerNumber());
            
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
    
    // ============ INSERT-ONLY UPDATE ENDPOINTS ============
    
    /**
     * Update customer address (INSERT-ONLY: creates new version with U operation)
     */
    @PutMapping("/customer/{customerNumber}/address")
    @Operation(
        summary = "Update customer address",
        description = "Updates customer address by creating a new version of the customer record (INSERT-ONLY paradigm). The original record is preserved for audit trail."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Address updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserProfileResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n" +
                            "  \"customerId\": \"550e8400-e29b-41d4-a716-446655440000\",\n" +
                            "  \"customerNumber\": \"CUST-20251024-000001\",\n" +
                            "  \"crudOperation\": \"U\",\n" +
                            "  \"versionTimestamp\": \"2025-01-24T10:35:00\",\n" +
                            "  \"userId\": \"user123\",\n" +
                            "  \"email\": \"customer@example.com\",\n" +
                            "  \"firstName\": \"John\",\n" +
                            "  \"lastName\": \"Doe\",\n" +
                            "  \"addressLine1\": \"456 New Address Street\",\n" +
                            "  \"addressLine2\": \"Apt 789\",\n" +
                            "  \"city\": \"Bangalore\",\n" +
                            "  \"state\": \"Karnataka\",\n" +
                            "  \"country\": \"India\",\n" +
                            "  \"postalCode\": \"560001\",\n" +
                            "  \"customerStatus\": \"ACTIVE\",\n" +
                            "  \"kycStatus\": \"COMPLETED\"\n" +
                            "}"
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid address data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<?> updateAddress(
            @Parameter(description = "Customer number (e.g., CUST-20251024-000001)", required = true)
            @PathVariable String customerNumber,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated address information",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = UpdateAddressRequest.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        value = "{\n" +
                                "  \"addressLine1\": \"456 New Address Street\",\n" +
                                "  \"addressLine2\": \"Apt 789\",\n" +
                                "  \"city\": \"Bangalore\",\n" +
                                "  \"state\": \"Karnataka\",\n" +
                                "  \"country\": \"India\",\n" +
                                "  \"postalCode\": \"560001\"\n" +
                                "}"
                    )
                )
            )
            @RequestBody UpdateAddressRequest request,
            HttpServletRequest httpRequest) {
        try {
            // Find customer by customer number first
            Customer customer = customerService.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new RuntimeException("Customer profile not found for customerNumber: " + customerNumber));
            
            // Authorization check: customers can only update their own data, admins can update all
            if (!isAuthorized(httpRequest, customer.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: You can only update your own profile");
            }
            
            // Update address fields
            customer.setAddressLine1(request.getAddressLine1());
            customer.setAddressLine2(request.getAddressLine2());
            customer.setCity(request.getCity());
            customer.setState(request.getState());
            customer.setCountry(request.getCountry());
            customer.setPostalCode(request.getPostalCode());
            
            // This will create a new row with CrudOperation = U
            Customer updatedCustomer = customerService.updateCustomer(customer);
            
            // Update address components (create new versions)
            updateAddressComponents(updatedCustomer, request);
            
            UserProfileResponse response = createUserProfileResponse(updatedCustomer);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Update customer name (INSERT-ONLY: creates new version with U operation)
     */
    @PutMapping("/customer/{customerNumber}/name")
    @Operation(
        summary = "Update customer name",
        description = "Updates customer name by creating a new version of the customer record (INSERT-ONLY paradigm). The original record is preserved for audit trail."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Name updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserProfileResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n" +
                            "  \"customerId\": \"550e8400-e29b-41d4-a716-446655440000\",\n" +
                            "  \"customerNumber\": \"CUST-20251024-000001\",\n" +
                            "  \"crudOperation\": \"U\",\n" +
                            "  \"versionTimestamp\": \"2025-01-24T10:40:00\",\n" +
                            "  \"userId\": \"user123\",\n" +
                            "  \"email\": \"customer@example.com\",\n" +
                            "  \"firstName\": \"Jane\",\n" +
                            "  \"middleName\": \"Marie\",\n" +
                            "  \"lastName\": \"Smith\",\n" +
                            "  \"phoneNumber\": \"+1234567890\",\n" +
                            "  \"customerStatus\": \"ACTIVE\",\n" +
                            "  \"kycStatus\": \"COMPLETED\"\n" +
                            "}"
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid name data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<?> updateName(
            @Parameter(description = "Customer number (e.g., CUST-20251024-000001)", required = true)
            @PathVariable String customerNumber,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated name information",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = UpdateNameRequest.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        value = "{\n" +
                                "  \"firstName\": \"Jane\",\n" +
                                "  \"middleName\": \"Marie\",\n" +
                                "  \"lastName\": \"Smith\"\n" +
                                "}"
                    )
                )
            )
            @RequestBody UpdateNameRequest request,
            HttpServletRequest httpRequest) {
        try {
            // Find customer by customer number first
            Customer customer = customerService.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new RuntimeException("Customer profile not found for customerNumber: " + customerNumber));
            
            // Authorization check: customers can only update their own data, admins can update all
            if (!isAuthorized(httpRequest, customer.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: You can only update your own profile");
            }
            
            // This will create a new row with CrudOperation = U
            Customer updatedCustomer = customerService.updateCustomer(customer);
            
            // Update name components (create new versions)
            if (request.getFirstName() != null) {
                CustomerNameComponent firstName = new CustomerNameComponent();
                firstName.setCustomer(updatedCustomer);
                firstName.setNameComponentType(CustomerNameComponent.NameComponentType.FIRST_NAME);
                firstName.setNameValue(request.getFirstName());
                firstName.setEffectiveDate(LocalDateTime.now());
                firstName.setCrudOperation(CustomerNameComponent.CrudOperation.U);
                firstName.setVersionTimestamp(LocalDateTime.now());
                firstName.setCustomerNumber(updatedCustomer.getCustomerNumber());
                nameComponentService.save(firstName);
            }
            
            if (request.getMiddleName() != null) {
                CustomerNameComponent middleName = new CustomerNameComponent();
                middleName.setCustomer(updatedCustomer);
                middleName.setNameComponentType(CustomerNameComponent.NameComponentType.MIDDLE_NAME);
                middleName.setNameValue(request.getMiddleName());
                middleName.setEffectiveDate(LocalDateTime.now());
                middleName.setCrudOperation(CustomerNameComponent.CrudOperation.U);
                middleName.setVersionTimestamp(LocalDateTime.now());
                middleName.setCustomerNumber(updatedCustomer.getCustomerNumber());
                nameComponentService.save(middleName);
            }
            
            if (request.getLastName() != null) {
                CustomerNameComponent lastName = new CustomerNameComponent();
                lastName.setCustomer(updatedCustomer);
                lastName.setNameComponentType(CustomerNameComponent.NameComponentType.LAST_NAME);
                lastName.setNameValue(request.getLastName());
                lastName.setEffectiveDate(LocalDateTime.now());
                lastName.setCrudOperation(CustomerNameComponent.CrudOperation.U);
                lastName.setVersionTimestamp(LocalDateTime.now());
                lastName.setCustomerNumber(updatedCustomer.getCustomerNumber());
                nameComponentService.save(lastName);
            }
            
            UserProfileResponse response = createUserProfileResponse(updatedCustomer);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Update customer identification documents (INSERT-ONLY: creates new version with U operation)
     */
    @PutMapping("/customer/{customerNumber}/identification")
    @Operation(
        summary = "Update customer identification documents",
        description = "Updates customer identification documents by creating a new version of the customer record (INSERT-ONLY paradigm). The original record is preserved for audit trail."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Identification documents updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserProfileResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\n" +
                            "  \"customerId\": \"550e8400-e29b-41d4-a716-446655440000\",\n" +
                            "  \"customerNumber\": \"CUST-20251024-000001\",\n" +
                            "  \"crudOperation\": \"U\",\n" +
                            "  \"versionTimestamp\": \"2025-01-24T10:45:00\",\n" +
                            "  \"userId\": \"user123\",\n" +
                            "  \"email\": \"customer@example.com\",\n" +
                            "  \"firstName\": \"John\",\n" +
                            "  \"lastName\": \"Doe\",\n" +
                            "  \"aadharNumber\": \"1234-5678-9012\",\n" +
                            "  \"maskedAadhar\": \"XXXX-XXXX-9012\",\n" +
                            "  \"panNumber\": \"ABCDE1234F\",\n" +
                            "  \"maskedPan\": \"ABXXXX34F\",\n" +
                            "  \"passportNumber\": \"K1234567\",\n" +
                            "  \"drivingLicense\": \"DL-1234567890\",\n" +
                            "  \"customerStatus\": \"ACTIVE\",\n" +
                            "  \"kycStatus\": \"COMPLETED\"\n" +
                            "}"
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid identification data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<?> updateIdentification(
            @Parameter(description = "Customer number (e.g., CUST-20251024-000001)", required = true)
            @PathVariable String customerNumber,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated identification documents",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = UpdateIdentificationRequest.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        value = "{\n" +
                                "  \"aadharNumber\": \"1234-5678-9012\",\n" +
                                "  \"panNumber\": \"ABCDE1234F\",\n" +
                                "  \"passportNumber\": \"K1234567\",\n" +
                                "  \"drivingLicense\": \"DL-1234567890\"\n" +
                                "}"
                    )
                )
            )
            @RequestBody UpdateIdentificationRequest request,
            HttpServletRequest httpRequest) {
        try {
            // Find customer by customer number first
            Customer customer = customerService.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new RuntimeException("Customer profile not found for customerNumber: " + customerNumber));
            
            // Authorization check: customers can only update their own data, admins can update all
            if (!isAuthorized(httpRequest, customer.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: You can only update your own profile");
            }
            
            // This will create a new row with CrudOperation = U
            Customer updatedCustomer = customerService.updateCustomer(customer);
            
            // Update identification documents (create new versions)
            if (request.getAadharNumber() != null) {
                CustomerIdentification aadhar = new CustomerIdentification();
                aadhar.setCustomer(updatedCustomer);
                aadhar.setIdentificationType(CustomerIdentification.AADHAR_CARD);
                aadhar.setIdentificationItem(request.getAadharNumber());
                aadhar.setEffectiveDate(LocalDateTime.now());
                aadhar.setCrudOperation(CustomerIdentification.CrudOperation.U);
                aadhar.setVersionTimestamp(LocalDateTime.now());
                identificationService.save(aadhar);
            }
            
            if (request.getPanNumber() != null) {
                CustomerIdentification pan = new CustomerIdentification();
                pan.setCustomer(updatedCustomer);
                pan.setIdentificationType(CustomerIdentification.PAN_CARD);
                pan.setIdentificationItem(request.getPanNumber());
                pan.setEffectiveDate(LocalDateTime.now());
                pan.setCrudOperation(CustomerIdentification.CrudOperation.U);
                pan.setVersionTimestamp(LocalDateTime.now());
                identificationService.save(pan);
            }
            
            if (request.getPassportNumber() != null) {
                CustomerIdentification passport = new CustomerIdentification();
                passport.setCustomer(updatedCustomer);
                passport.setIdentificationType(CustomerIdentification.PASSPORT);
                passport.setIdentificationItem(request.getPassportNumber());
                passport.setEffectiveDate(LocalDateTime.now());
                passport.setCrudOperation(CustomerIdentification.CrudOperation.U);
                passport.setVersionTimestamp(LocalDateTime.now());
                identificationService.save(passport);
            }
            
            if (request.getDrivingLicense() != null) {
                CustomerIdentification license = new CustomerIdentification();
                license.setCustomer(updatedCustomer);
                license.setIdentificationType(CustomerIdentification.DRIVING_LICENSE);
                license.setIdentificationItem(request.getDrivingLicense());
                license.setEffectiveDate(LocalDateTime.now());
                license.setCrudOperation(CustomerIdentification.CrudOperation.U);
                license.setVersionTimestamp(LocalDateTime.now());
                identificationService.save(license);
            }
            
            UserProfileResponse response = createUserProfileResponse(updatedCustomer);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Get customer audit trail (all versions)
     */
    @GetMapping("/user/{userId}/audit-trail")
    @Operation(
        summary = "Get customer audit trail",
        description = "Returns all versions of the customer record including create, update, and delete operations for audit purposes."
    )
    public ResponseEntity<?> getAuditTrail(@PathVariable String userId) {
        try {
            // First find customer number by userId
            Optional<String> customerNumberOpt = customerService.findByUserId(userId)
                .map(Customer::getCustomerNumber);
            
            if (customerNumberOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Customer profile not found for userId: " + userId);
            }
            
            List<Customer> versions = customerService.findAllVersionsByCustomerNumber(customerNumberOpt.get());
            
            // Map to simplified audit trail response
            List<Object> auditTrail = versions.stream()
                .map(v -> {
                    return new Object() {
                        public final String customerId = v.getCustomerId();
                        public final String customerNumber = v.getCustomerNumber();
                        public final String crudOperation = v.getCrudOperation().toString();
                        public final LocalDateTime versionTimestamp = v.getVersionTimestamp();
                        public final String email = v.getEmailId();
                        public final String phoneNumber = v.getPhoneNumber();
                    };
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(auditTrail);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    
    /**
     * Helper method to update address components
     */
    private void updateAddressComponents(Customer customer, UpdateAddressRequest request) {
        if (request.getAddressLine1() != null) {
            CustomerAddressComponent component = new CustomerAddressComponent();
            component.setCustomer(customer);
            component.setAddressComponentType(CustomerAddressComponent.AddressComponentType.ADDRESS_LINE_1);
            component.setAddressValue(request.getAddressLine1());
            component.setEffectiveDate(LocalDateTime.now());
            component.setCrudOperation(CustomerAddressComponent.CrudOperation.U);
            component.setVersionTimestamp(LocalDateTime.now());
            component.setCustomerNumber(customer.getCustomerNumber());
            addressComponentService.save(component);
        }
        
        if (request.getAddressLine2() != null) {
            CustomerAddressComponent component = new CustomerAddressComponent();
            component.setCustomer(customer);
            component.setAddressComponentType(CustomerAddressComponent.AddressComponentType.ADDRESS_LINE_2);
            component.setAddressValue(request.getAddressLine2());
            component.setEffectiveDate(LocalDateTime.now());
            component.setCrudOperation(CustomerAddressComponent.CrudOperation.U);
            component.setVersionTimestamp(LocalDateTime.now());
            component.setCustomerNumber(customer.getCustomerNumber());
            addressComponentService.save(component);
        }
        
        if (request.getCity() != null) {
            CustomerAddressComponent component = new CustomerAddressComponent();
            component.setCustomer(customer);
            component.setAddressComponentType(CustomerAddressComponent.AddressComponentType.CITY);
            component.setAddressValue(request.getCity());
            component.setEffectiveDate(LocalDateTime.now());
            component.setCrudOperation(CustomerAddressComponent.CrudOperation.U);
            component.setVersionTimestamp(LocalDateTime.now());
            component.setCustomerNumber(customer.getCustomerNumber());
            addressComponentService.save(component);
        }
        
        if (request.getState() != null) {
            CustomerAddressComponent component = new CustomerAddressComponent();
            component.setCustomer(customer);
            component.setAddressComponentType(CustomerAddressComponent.AddressComponentType.STATE);
            component.setAddressValue(request.getState());
            component.setEffectiveDate(LocalDateTime.now());
            component.setCrudOperation(CustomerAddressComponent.CrudOperation.U);
            component.setVersionTimestamp(LocalDateTime.now());
            component.setCustomerNumber(customer.getCustomerNumber());
            addressComponentService.save(component);
        }
        
        if (request.getCountry() != null) {
            CustomerAddressComponent component = new CustomerAddressComponent();
            component.setCustomer(customer);
            component.setAddressComponentType(CustomerAddressComponent.AddressComponentType.COUNTRY);
            component.setAddressValue(request.getCountry());
            component.setEffectiveDate(LocalDateTime.now());
            component.setCrudOperation(CustomerAddressComponent.CrudOperation.U);
            component.setVersionTimestamp(LocalDateTime.now());
            component.setCustomerNumber(customer.getCustomerNumber());
            addressComponentService.save(component);
        }
        
        if (request.getPostalCode() != null) {
            CustomerAddressComponent component = new CustomerAddressComponent();
            component.setCustomer(customer);
            component.setAddressComponentType(CustomerAddressComponent.AddressComponentType.POSTAL_CODE);
            component.setAddressValue(request.getPostalCode());
            component.setEffectiveDate(LocalDateTime.now());
            component.setCrudOperation(CustomerAddressComponent.CrudOperation.U);
            component.setVersionTimestamp(LocalDateTime.now());
            component.setCustomerNumber(customer.getCustomerNumber());
            addressComponentService.save(component);
        }
    }
    
    /**
     * Helper method to create UserProfileResponse from normalized Customer data
     */
    private UserProfileResponse createUserProfileResponse(Customer customer) {
        UserProfileResponse response = new UserProfileResponse();
        
        // INSERT-ONLY paradigm fields
        response.setCustomerId(customer.getCustomerId());
        response.setCustomerNumber(customer.getCustomerNumber());
        response.setCrudOperation(customer.getCrudOperation() != null ? customer.getCrudOperation().name() : null);
        response.setVersionTimestamp(customer.getVersionTimestamp());
        
        // Basic customer data
        response.setUserId(customer.getUserId());
        response.setEmail(customer.getEmailId());
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
        
        // Customer status
        response.setCustomerType(customer.getCustomerType() != null ? customer.getCustomerType().name() : null);
        response.setCustomerStatus(customer.getCustomerStatus() != null ? customer.getCustomerStatus().name() : null);
        response.setKycStatus(customer.getKycStatus() != null ? customer.getKycStatus().name() : null);
        response.setKycCompletionDate(customer.getKycCompletionDate());
        
    // Get name from normalized table using customerNumber (business id)
    List<CustomerNameComponent> nameComponents = nameComponentService.findByCustomerNumber(customer.getCustomerNumber());
        for (CustomerNameComponent nameComponent : nameComponents) {
            if (CustomerNameComponent.NameComponentType.FIRST_NAME.equals(nameComponent.getNameComponentType())) {
                response.setFirstName(nameComponent.getNameValue());
            } else if (CustomerNameComponent.NameComponentType.LAST_NAME.equals(nameComponent.getNameComponentType())) {
                response.setLastName(nameComponent.getNameValue());
            } else if (CustomerNameComponent.NameComponentType.MIDDLE_NAME.equals(nameComponent.getNameComponentType())) {
                response.setMiddleName(nameComponent.getNameValue());
            }
        }
        
    // Get address from normalized table using customerNumber (business id)
    List<CustomerAddressComponent> addressComponents = addressComponentService.findByCustomerNumber(customer.getCustomerNumber());
        for (CustomerAddressComponent addressComponent : addressComponents) {
            if (CustomerAddressComponent.AddressComponentType.ADDRESS_LINE_1.equals(addressComponent.getAddressComponentType())) {
                response.setAddressLine1(addressComponent.getAddressValue());
            } else if (CustomerAddressComponent.AddressComponentType.ADDRESS_LINE_2.equals(addressComponent.getAddressComponentType())) {
                response.setAddressLine2(addressComponent.getAddressValue());
            } else if (CustomerAddressComponent.AddressComponentType.CITY.equals(addressComponent.getAddressComponentType())) {
                response.setCity(addressComponent.getAddressValue());
            } else if (CustomerAddressComponent.AddressComponentType.STATE.equals(addressComponent.getAddressComponentType())) {
                response.setState(addressComponent.getAddressValue());
            } else if (CustomerAddressComponent.AddressComponentType.COUNTRY.equals(addressComponent.getAddressComponentType())) {
                response.setCountry(addressComponent.getAddressValue());
            } else if (CustomerAddressComponent.AddressComponentType.POSTAL_CODE.equals(addressComponent.getAddressComponentType())) {
                response.setPostalCode(addressComponent.getAddressValue());
            }
        }
        
    // Get identification numbers from normalized table using customerNumber (business id)
    List<CustomerIdentification> identifications = identificationService.findByCustomerNumber(customer.getCustomerNumber());
        for (CustomerIdentification id : identifications) {
            if (CustomerIdentification.AADHAR_CARD.equals(id.getIdentificationType())) {
                String aadharNumber = id.getIdentificationItem();
                response.setAadharNumber(aadharNumber);
                // Set masked Aadhar
                if (aadharNumber != null && aadharNumber.length() >= 4) {
                    response.setMaskedAadhar("XXXX-XXXX-" + aadharNumber.substring(aadharNumber.length() - 4));
                }
            } else if (CustomerIdentification.PAN_CARD.equals(id.getIdentificationType())) {
                String panNumber = id.getIdentificationItem();
                response.setPanNumber(panNumber);
                // Set masked PAN
                if (panNumber != null && panNumber.length() >= 4) {
                    response.setMaskedPan(panNumber.substring(0, 2) + "XXXX" + panNumber.substring(panNumber.length() - 2));
                }
            } else if (CustomerIdentification.PASSPORT.equals(id.getIdentificationType())) {
                response.setPassportNumber(id.getIdentificationItem());
            } else if (CustomerIdentification.DRIVING_LICENSE.equals(id.getIdentificationType())) {
                response.setDrivingLicense(id.getIdentificationItem());
            }
        }
        
        return response;
    }
}