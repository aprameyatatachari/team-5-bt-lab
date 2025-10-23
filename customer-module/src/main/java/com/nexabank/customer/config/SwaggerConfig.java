package com.nexabank.customer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI Configuration for Customer Module
 * Provides comprehensive API documentation with security configuration
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:1005}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:" + serverPort);
        devServer.setDescription("Customer Module Local Development Server");

        Server prodServer = new Server();
        prodServer.setUrl("https://api.nexabank.com/customer");
        prodServer.setDescription("Customer Module Production Server");

        Contact contact = new Contact();
        contact.setName("NEXA Bank Customer Service Support");
        contact.setUrl("https://www.nexabank.com");
        contact.setEmail("customer-service@nexabank.com");

        License license = new License()
                .name("Proprietary")
                .url("https://www.nexabank.com/license");

        String description = """
                ## Overview
                The Customer Profile Management API provides comprehensive customer data management services for NEXA Bank's microservices architecture with full audit trail capabilities.
                
                ## Key Features
                - **INSERT-ONLY Paradigm**: Banking-grade audit trail with zero data loss
                - **Versioned Customer Records**: Complete history of all customer changes
                - **Normalized Data Storage**: Separate tables for names, addresses, and identifications
                - **Customer Number System**: Unique business identifier across all versions
                - **CRUD Operation Tracking**: Every change tagged with C (Create), U (Update), or D (Delete)
                - **Soft Delete**: Customer deletions preserved for compliance and audit
                - **Audit Trail Access**: View complete customer history
                - **JWT Authentication**: Secure access via Auth Service tokens
                
                ## INSERT-ONLY Architecture
                
                ### Core Concept
                In banking systems, data integrity and audit trails are critical. Instead of updating or deleting records, we create new versions:
                
                **Traditional Approach (NOT USED):**
                ```sql
                UPDATE customers SET address = 'New Address' WHERE id = 123;  -- Old data lost!
                DELETE FROM customers WHERE id = 123;  -- Data gone forever!
                ```
                
                **INSERT-ONLY Approach (IMPLEMENTED):**
                ```sql
                -- Update: Insert new row with same customer_number
                INSERT INTO customers (customer_number, address, crud_operation, version_timestamp)
                VALUES ('CUST-20251023-000001', 'New Address', 'U', NOW());
                
                -- Delete: Insert new row with D operation
                INSERT INTO customers (customer_number, crud_operation, version_timestamp)
                VALUES ('CUST-20251023-000001', 'D', NOW());
                ```
                
                ### Benefits
                1. **Complete Audit Trail**: Every change is recorded with timestamp
                2. **Regulatory Compliance**: Meets banking audit requirements
                3. **Time Travel**: View customer state at any point in history
                4. **Rollback Capability**: Can restore previous versions if needed
                5. **Fraud Detection**: Track suspicious pattern of changes
                6. **Dispute Resolution**: Prove what data existed at specific times
                
                ## Customer Number System
                
                ### Format
                Customer numbers follow the pattern: `CUST-YYYYMMDD-XXXXXX`
                
                **Example:** `CUST-20251023-000001`
                - `CUST`: Prefix indicating customer
                - `20251023`: Date created (October 23, 2025)
                - `000001`: Sequential number for that day
                
                ### Key Properties
                - **Unique**: Each customer gets one number on creation
                - **Persistent**: Same number across all versions
                - **Searchable**: Easy to find all versions of a customer
                - **Human-Readable**: Date helps with support and debugging
                
                ## CRUD Operations
                
                ### C - Create (Initial Registration)
                When a customer is first created:
                ```json
                {
                  "customerId": "uuid-v4-abc123",
                  "customerNumber": "CUST-20251023-000001",
                  "crudOperation": "C",
                  "versionTimestamp": "2025-10-23T10:00:00Z",
                  "firstName": "John",
                  "email": "john@example.com"
                }
                ```
                
                ### U - Update (Address/Name/ID Change)
                When customer updates their address:
                ```json
                {
                  "customerId": "uuid-v4-def456",  // NEW ID
                  "customerNumber": "CUST-20251023-000001",  // SAME NUMBER
                  "crudOperation": "U",
                  "versionTimestamp": "2025-10-23T14:30:00Z",
                  "firstName": "John",
                  "email": "john@example.com",
                  "address": "123 New Street"  // Updated field
                }
                ```
                
                ### D - Delete (Soft Delete)
                When customer account is closed:
                ```json
                {
                  "customerId": "uuid-v4-ghi789",  // NEW ID
                  "customerNumber": "CUST-20251023-000001",  // SAME NUMBER
                  "crudOperation": "D",
                  "versionTimestamp": "2025-10-23T16:00:00Z",
                  "customerStatus": "CLOSED"
                }
                ```
                
                **Important:** All previous versions remain in database!
                
                ## Normalized Data Model
                
                ### Main Tables
                
                **1. customers**
                - Core customer information
                - Multiple rows per customer (one per version)
                - Linked by `customer_number`
                
                **2. customer_name_components**
                - First name, middle name, last name separated
                - Tracks name changes over time
                - Supports international name formats
                
                **3. customer_address_components**
                - Address fields stored separately
                - City, state, postal code as individual components
                - Enables address history tracking
                
                **4. customer_identification**
                - Aadhar, PAN, Passport, Driving License
                - Document verification status
                - Expiration tracking
                
                ### Data Retrieval
                
                **Get Latest Version (Default):**
                ```sql
                SELECT * FROM customers 
                WHERE customer_number = 'CUST-20251023-000001'
                  AND crud_operation != 'D'
                ORDER BY version_timestamp DESC
                LIMIT 1;
                ```
                
                **Get Audit Trail:**
                ```sql
                SELECT * FROM customers 
                WHERE customer_number = 'CUST-20251023-000001'
                ORDER BY version_timestamp DESC;
                ```
                
                ## API Endpoints
                
                ### Profile Management
                - **POST /api/profiles** - Create customer profile (called by Auth Service)
                - **GET /api/profiles/user/{userId}** - Get customer by user ID (latest version)
                - **GET /api/profiles/email/{email}** - Get customer by email
                - **GET /api/profiles** - Get all customers (latest versions only)
                - **GET /api/profiles/search?name={name}** - Search customers by name
                
                ### INSERT-ONLY Updates
                - **PUT /api/profiles/user/{userId}/address** - Update address (creates new version)
                - **PUT /api/profiles/user/{userId}/name** - Update name (creates new version)
                - **PUT /api/profiles/user/{userId}/identification** - Update ID docs (creates new version)
                
                ### Audit & History
                - **GET /api/profiles/user/{userId}/audit-trail** - View all versions and changes
                
                ### Soft Delete
                - **DELETE /api/profiles/user/{userId}** - Close account (creates D version)
                
                ## Authentication
                
                All endpoints require JWT token from Auth Service:
                ```
                Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
                ```
                
                The Auth Service (port 8080) issues tokens after login. Customer Service validates these tokens using the public key from Auth Service.
                
                ## Integration with Auth Service
                
                ### Registration Flow
                1. User submits registration to Auth Service
                2. Auth Service creates auth record (email + password)
                3. Auth Service calls Customer Service `/api/profiles` endpoint
                4. Customer Service creates customer profile with `crud_operation = 'C'`
                5. Customer Number generated: `CUST-20251023-000001`
                6. User receives JWT token
                
                ### Profile Retrieval Flow
                1. Client sends request with JWT token
                2. Customer Service validates token (via Auth Service public key)
                3. Extracts userId from token
                4. Queries for latest non-deleted version
                5. Joins with name/address/identification tables
                6. Returns complete profile
                
                ## Use Case Examples
                
                ### Example 1: Customer Address Change
                
                **Scenario:** Customer moves to a new address
                
                **Request:**
                ```bash
                PUT /api/profiles/user/usr_123/address
                Authorization: Bearer {token}
                Content-Type: application/json
                
                {
                  "addressLine1": "456 New Street",
                  "addressLine2": "Apt 5B",
                  "city": "Mumbai",
                  "state": "Maharashtra",
                  "country": "India",
                  "postalCode": "400001"
                }
                ```
                
                **What Happens:**
                1. System finds latest customer version
                2. Creates new customer row with same `customer_number`
                3. Sets `crud_operation = 'U'`
                4. Sets `version_timestamp = NOW()`
                5. Creates new address component entries
                6. Returns updated profile
                
                **Database State:**
                ```
                Row 1: customer_number=CUST-001, crud_operation=C, address="Old Street"
                Row 2: customer_number=CUST-001, crud_operation=U, address="New Street"  ← Latest
                ```
                
                ### Example 2: Name Change After Marriage
                
                **Request:**
                ```bash
                PUT /api/profiles/user/usr_123/name
                Authorization: Bearer {token}
                
                {
                  "firstName": "Jane",
                  "middleName": "Kumar",
                  "lastName": "Sharma"
                }
                ```
                
                **Database Changes:**
                - New customer row created (U operation)
                - New name_component rows for each name part
                - Old versions preserved for audit
                
                ### Example 3: Account Closure
                
                **Request:**
                ```bash
                DELETE /api/profiles/user/usr_123
                Authorization: Bearer {token}
                ```
                
                **What Happens:**
                1. New row created with `crud_operation = 'D'`
                2. `customer_status = 'CLOSED'`
                3. All previous versions remain in database
                4. GET requests will not return this customer (filtered by crud_operation != 'D')
                5. Audit trail still shows complete history
                
                ### Example 4: Audit Trail Review
                
                **Request:**
                ```bash
                GET /api/profiles/user/usr_123/audit-trail
                Authorization: Bearer {token}
                ```
                
                **Response:**
                ```json
                [
                  {
                    "customerId": "uuid-4",
                    "customerNumber": "CUST-20251023-000001",
                    "crudOperation": "D",
                    "versionTimestamp": "2025-10-23T16:00:00Z",
                    "email": "john@example.com",
                    "phoneNumber": "9876543210"
                  },
                  {
                    "customerId": "uuid-3",
                    "customerNumber": "CUST-20251023-000001",
                    "crudOperation": "U",
                    "versionTimestamp": "2025-10-23T14:30:00Z",
                    "email": "john@example.com",
                    "phoneNumber": "9876543210"
                  },
                  {
                    "customerId": "uuid-2",
                    "customerNumber": "CUST-20251023-000001",
                    "crudOperation": "U",
                    "versionTimestamp": "2025-10-23T12:00:00Z",
                    "email": "john@example.com",
                    "phoneNumber": "9876543210"
                  },
                  {
                    "customerId": "uuid-1",
                    "customerNumber": "CUST-20251023-000001",
                    "crudOperation": "C",
                    "versionTimestamp": "2025-10-23T10:00:00Z",
                    "email": "john@example.com",
                    "phoneNumber": "9876543210"
                  }
                ]
                ```
                
                ## Data Model Example
                
                ### Customer Journey Timeline
                
                **Day 1 - 10:00 AM: Registration (C)**
                ```json
                {
                  "customerNumber": "CUST-20251023-000001",
                  "crudOperation": "C",
                  "firstName": "John",
                  "lastName": "Doe",
                  "email": "john@example.com",
                  "phoneNumber": "9876543210",
                  "address": "123 Old Street",
                  "city": "Delhi"
                }
                ```
                
                **Day 5 - 2:30 PM: Address Update (U)**
                ```json
                {
                  "customerNumber": "CUST-20251023-000001",
                  "crudOperation": "U",
                  "firstName": "John",
                  "lastName": "Doe",
                  "email": "john@example.com",
                  "phoneNumber": "9876543210",
                  "address": "456 New Street",  // Changed
                  "city": "Mumbai"  // Changed
                }
                ```
                
                **Day 10 - 11:00 AM: Name Update (U)**
                ```json
                {
                  "customerNumber": "CUST-20251023-000001",
                  "crudOperation": "U",
                  "firstName": "John",
                  "lastName": "Smith",  // Changed (marriage)
                  "email": "john@example.com",
                  "phoneNumber": "9876543210",
                  "address": "456 New Street",
                  "city": "Mumbai"
                }
                ```
                
                **Day 30 - 4:00 PM: Account Closed (D)**
                ```json
                {
                  "customerNumber": "CUST-20251023-000001",
                  "crudOperation": "D",
                  "customerStatus": "CLOSED",
                  "firstName": "John",
                  "lastName": "Smith",
                  "email": "john@example.com"
                }
                ```
                
                **All 4 rows preserved forever!**
                
                ## Error Handling
                
                ### Standard Error Response
                ```json
                {
                  "timestamp": "2025-10-23T10:30:00Z",
                  "status": 400,
                  "error": "Bad Request",
                  "message": "Customer profile already exists for user: usr_123",
                  "path": "/api/profiles"
                }
                ```
                
                ### Common Error Codes
                - **400 Bad Request**: Invalid input, customer already exists
                - **401 Unauthorized**: Missing or invalid JWT token
                - **404 Not Found**: Customer not found or deleted
                - **500 Internal Server Error**: Server-side error
                
                ## Best Practices
                
                ### For Frontend Developers
                1. **Always use latest version**: Don't cache old customer data
                2. **Handle 404 gracefully**: Customer might be deleted
                3. **Show update confirmations**: Inform users about profile changes
                4. **Audit trail access**: Provide history view for customer service
                
                ### For Backend Developers
                1. **Never UPDATE/DELETE**: Always INSERT new rows
                2. **Filter by crudOperation**: Exclude 'D' in normal queries
                3. **Index customerNumber**: Critical for performance
                4. **Preserve all data**: Even if fields are null in updates
                
                ### For Database Administrators
                1. **Partition by date**: Use version_timestamp for partitioning
                2. **Archive old versions**: Move to separate table after 7 years
                3. **Regular backups**: Critical for compliance
                4. **Monitor table growth**: INSERT-ONLY means continuous growth
                
                ## Compliance & Security
                
                ### Regulatory Compliance
                - **RBI Guidelines**: Full audit trail as required
                - **Data Retention**: 7 years minimum (configurable)
                - **Right to be Forgotten**: Soft delete maintains compliance
                - **Audit Reports**: All changes tracked with timestamps
                
                ### Security Features
                - **JWT Authentication**: All endpoints protected
                - **Role-Based Access**: Customer can only update their own data
                - **Audit Logging**: All API calls logged
                - **Data Encryption**: Sensitive fields encrypted at rest
                
                ## Performance Considerations
                
                ### Query Optimization
                - Index on `customer_number` for fast lookups
                - Index on `(customer_number, version_timestamp DESC)` for latest version
                - Index on `(crud_operation, version_timestamp)` for active customers
                
                ### Scalability
                - Read replicas for heavy read workloads
                - Caching layer for frequently accessed profiles
                - Async processing for audit trail queries
                - Database partitioning by time period
                
                ## Support
                For API support, integration help, or reporting issues:
                - Email: customer-service@nexabank.com
                - Documentation: https://www.nexabank.com/api-docs/customer
                - Status: https://status.nexabank.com
                """;

        Info info = new Info()
                .title("NEXA Bank - Customer Profile Management API")
                .version("2.0.0")
                .contact(contact)
                .description(description)
                .termsOfService("https://www.nexabank.com/terms")
                .license(license);

        Tag profileTag = new Tag()
                .name("Customer Profile Management")
                .description("CRUD operations for customer profile information with INSERT-ONLY paradigm");

        Tag updateTag = new Tag()
                .name("Profile Updates")
                .description("Specialized endpoints for updating address, name, and identification (creates new versions)");

        Tag auditTag = new Tag()
                .name("Audit Trail")
                .description("Access complete customer history and version tracking");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer))
                .tags(List.of(profileTag, updateTag, auditTag))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\"")));
    }
}