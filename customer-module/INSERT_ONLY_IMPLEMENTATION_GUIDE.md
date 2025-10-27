# INSERT-ONLY Pattern Implementation Guide

## Overview
This guide provides a complete checklist for converting a Spring Boot microservice to use the INSERT-ONLY audit trail pattern. Follow these steps carefully to avoid common pitfalls.

---

## Prerequisites
- Spring Boot application with JPA/Hibernate
- Entities with normalized structure (main entity + component entities)
- Database with foreign key relationships

---

## Phase 1: Entity Layer Updates

### 1.1 Main Entity (e.g., Customer)

Add these fields to your main entity:

```java
// INSERT-ONLY audit trail fields
@Enumerated(EnumType.STRING)
@Column(name = "crud_operation", nullable = false)
private CrudOperation crudOperation = CrudOperation.C;

@Column(name = "version_timestamp", nullable = false)
private LocalDateTime versionTimestamp;

// CRUD Operation enum
public enum CrudOperation {
    C, // Create
    U, // Update
    D  // Delete
}
```

Add **ONLY ONE** `@PrePersist` callback:

```java
@PrePersist
protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
        createdAt = now;
    }
    if (updatedAt == null) {
        updatedAt = now;
    }
    if (versionTimestamp == null) {
        versionTimestamp = now;
    }
    if (crudOperation == null) {
        crudOperation = CrudOperation.C;
    }
}
```

Add **ONLY ONE** `@PreUpdate` callback:

```java
@PreUpdate
protected void onUpdate() {
    updatedAt = LocalDateTime.now();
    // Note: versionTimestamp is set manually in service layer, not here
}
```

⚠️ **CRITICAL**: Never have multiple `@PrePersist` or `@PreUpdate` methods - this causes BeanCreationException!

---

### 1.2 Component Entities (e.g., NameComponent, AddressComponent)

Add these fields to each component entity:

```java
// INSERT-ONLY audit trail fields
@Enumerated(EnumType.STRING)
@Column(name = "crud_operation", nullable = false)
private CrudOperation crudOperation = CrudOperation.C;

@Column(name = "version_timestamp", nullable = false)
private LocalDateTime versionTimestamp;

// Business identifier to link components across versions
@Column(name = "customer_number")
private String customerNumber;

// CRUD Operation enum
public enum CrudOperation {
    C, // Create
    U, // Update
    D  // Delete
}
```

⚠️ **CRITICAL**: Use the **business identifier** (e.g., customerNumber), NOT the technical primary key (customerId), to link components across versions!

Add **ONLY ONE** `@PrePersist` and **ONLY ONE** `@PreUpdate` callback:

```java
@PrePersist
protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
        createdAt = now;
    }
    if (updatedAt == null) {
        updatedAt = now;
    }
    if (versionTimestamp == null) {
        versionTimestamp = now;
    }
    if (crudOperation == null) {
        crudOperation = CrudOperation.C;
    }
}

@PreUpdate
protected void onUpdate() {
    updatedAt = LocalDateTime.now();
    if (versionTimestamp == null) {
        versionTimestamp = LocalDateTime.now();
    }
}
```

---

## Phase 2: Repository Layer Updates

### 2.1 Main Entity Repository

Replace simple queries with native queries that filter out deleted records:

```java
// ❌ WRONG - Returns deleted records
Optional<Customer> findByCustomerNumber(String customerNumber);

// ✅ CORRECT - Returns latest non-deleted record
@Query(value = "SELECT * FROM customer WHERE customer_number = :customerNumber " +
               "AND crud_operation != 'D' " +
               "ORDER BY version_timestamp DESC LIMIT 1", 
       nativeQuery = true)
Optional<Customer> findLatestByCustomerNumber(@Param("customerNumber") String customerNumber);
```

Apply this pattern to ALL find methods:
- `findByUserId` → `findLatestByUserId`
- `findByEmail` → `findLatestByEmail`
- `findById` → `findLatestById`
- `existsByXxx` → Add `AND crud_operation != 'D'` condition
- `findAll` → Filter out `crud_operation = 'D'`

Add method to get audit trail (all versions):

```java
@Query(value = "SELECT * FROM customer WHERE customer_number = :customerNumber " +
               "ORDER BY version_timestamp DESC", 
       nativeQuery = true)
List<Customer> findAllVersionsByCustomerNumber(@Param("customerNumber") String customerNumber);
```

---

### 2.2 Component Entity Repositories

Add queries by business identifier (NOT by customerId):

```java
// ❌ WRONG - Uses mutable technical PK
List<NameComponent> findByCustomerCustomerId(String customerId);

// ✅ CORRECT - Uses stable business identifier
@Query(value = "SELECT * FROM customer_name_component " +
               "WHERE customer_number = :customerNumber " +
               "AND crud_operation != 'D' " +
               "ORDER BY version_timestamp DESC", 
       nativeQuery = true)
List<NameComponent> findByCustomerNumberOrderByVersionDesc(@Param("customerNumber") String customerNumber);
```

Add delete by business identifier:

```java
// Deletes all versions for a customer
void deleteByCustomerNumber(String customerNumber);
```

Keep the old `findByCustomerCustomerId` method for backward compatibility if needed, but prefer the new method.

---

## Phase 3: Service Layer Updates

### 3.1 Main Entity Service

**Create Operation** - Sets CrudOperation.C (handled by @PrePersist):

```java
public Customer createCustomer(Customer customer) {
    // @PrePersist will set crudOperation = C and versionTimestamp
    return customerRepository.save(customer);
}
```

**Update Operation** - Creates new row with CrudOperation.U:

```java
public Customer updateCustomer(Customer existingCustomer) {
    // Create a NEW customer object with same business identifier
    Customer newVersion = new Customer();
    newVersion.setCustomerNumber(existingCustomer.getCustomerNumber()); // SAME business ID
    newVersion.setUserId(existingCustomer.getUserId());
    // ... copy all other fields ...
    
    // Set INSERT-ONLY fields
    newVersion.setCrudOperation(CrudOperation.U);
    newVersion.setVersionTimestamp(LocalDateTime.now());
    
    // Save creates NEW row (customerId will be different)
    return customerRepository.save(newVersion);
}
```

⚠️ **CRITICAL**: Create a NEW object, don't modify the existing one! This ensures a new row is inserted.

**Delete Operation** - Creates new row with CrudOperation.D:

```java
public void deleteCustomer(String customerId) {
    Customer existingCustomer = customerRepository.findById(customerId)
        .orElseThrow(() -> new RuntimeException("Customer not found"));
    
    // Create a NEW customer object for delete marker
    Customer deleteVersion = new Customer();
    deleteVersion.setCustomerNumber(existingCustomer.getCustomerNumber());
    deleteVersion.setUserId(existingCustomer.getUserId());
    // ... copy all fields ...
    
    // Set as deleted
    deleteVersion.setCrudOperation(CrudOperation.D);
    deleteVersion.setVersionTimestamp(LocalDateTime.now());
    
    customerRepository.save(deleteVersion);
}
```

**Find Operations** - Use "latest" methods:

```java
// ❌ WRONG
public Optional<Customer> findByCustomerNumber(String customerNumber) {
    return customerRepository.findByCustomerNumber(customerNumber);
}

// ✅ CORRECT
public Optional<Customer> findByCustomerNumber(String customerNumber) {
    return customerRepository.findLatestByCustomerNumber(customerNumber);
}
```

---

### 3.2 Component Entity Services

Add methods to work with business identifier:

```java
// Find components by business identifier
@Transactional(readOnly = true)
public List<NameComponent> findByCustomerNumber(String customerNumber) {
    return nameComponentRepository.findByCustomerNumberOrderByVersionDesc(customerNumber);
}

// Delete components by business identifier
public void deleteByCustomerNumber(String customerNumber) {
    nameComponentRepository.deleteByCustomerNumber(customerNumber);
}

// Save with INSERT-ONLY tracking
public NameComponent save(NameComponent component) {
    // If versionTimestamp not set, it will be set by @PrePersist
    return nameComponentRepository.save(component);
}
```

---

## Phase 4: Controller Layer Updates

### 4.1 Create Operation

Set component business identifier when creating:

```java
@PostMapping
public ResponseEntity<?> create(@RequestBody CreateRequest request) {
    // Create main entity
    Customer customer = new Customer();
    customer.setUserId(request.getUserId());
    // ... set other fields ...
    Customer savedCustomer = customerService.createCustomer(customer);
    
    // Create components - SET BUSINESS IDENTIFIER!
    if (request.getFirstName() != null) {
        NameComponent name = new NameComponent();
        name.setCustomer(savedCustomer);
        name.setNameValue(request.getFirstName());
        name.setCustomerNumber(savedCustomer.getCustomerNumber()); // ✅ SET THIS!
        // crudOperation and versionTimestamp set by @PrePersist
        nameComponentService.save(name);
    }
    
    return ResponseEntity.ok(savedCustomer);
}
```

⚠️ **CRITICAL**: Always set `customerNumber` on components during creation!

---

### 4.2 Update Operation

Create new main entity version, then create new component versions:

```java
@PutMapping("/{customerNumber}")
public ResponseEntity<?> update(
        @PathVariable String customerNumber,
        @RequestBody UpdateRequest request) {
    
    // Find existing customer
    Customer existingCustomer = customerService.findByCustomerNumber(customerNumber)
        .orElseThrow(() -> new RuntimeException("Not found"));
    
    // Update fields on existing object
    existingCustomer.setEmail(request.getEmail());
    // ... update other fields ...
    
    // This creates NEW customer row with CrudOperation.U
    Customer updatedCustomer = customerService.updateCustomer(existingCustomer);
    
    // Create NEW component rows with CrudOperation.U
    if (request.getFirstName() != null) {
        NameComponent name = new NameComponent();
        name.setCustomer(updatedCustomer);
        name.setNameValue(request.getFirstName());
        name.setCrudOperation(NameComponent.CrudOperation.U);
        name.setVersionTimestamp(LocalDateTime.now());
        name.setCustomerNumber(updatedCustomer.getCustomerNumber()); // ✅ SET THIS!
        nameComponentService.save(name);
    }
    
    return ResponseEntity.ok(updatedCustomer);
}
```

⚠️ **CRITICAL**: 
- Set `crudOperation = U` on component updates
- Set `versionTimestamp = LocalDateTime.now()` on component updates
- Set `customerNumber` on component updates

---

### 4.3 Read Operations

Query components by business identifier, NOT by customerId:

```java
private UserResponse createResponse(Customer customer) {
    UserResponse response = new UserResponse();
    response.setUserId(customer.getUserId());
    response.setCustomerNumber(customer.getCustomerNumber());
    // ... set other fields ...
    
    // ✅ CORRECT - Query by business identifier
    List<NameComponent> nameComponents = 
        nameComponentService.findByCustomerNumber(customer.getCustomerNumber());
    
    for (NameComponent component : nameComponents) {
        if (component.getNameComponentType() == NameComponentType.FIRST_NAME) {
            response.setFirstName(component.getNameValue());
        }
    }
    
    return response;
}
```

⚠️ **CRITICAL**: Use `customer.getCustomerNumber()` NOT `customer.getCustomerId()` for component lookups!

---

### 4.4 Delete Operations

Use business identifier for component deletion:

```java
@DeleteMapping("/{userId}")
public ResponseEntity<?> delete(@PathVariable String userId) {
    Customer customer = customerService.findByUserId(userId)
        .orElseThrow(() -> new RuntimeException("Not found"));
    
    // ✅ Delete components by business identifier
    nameComponentService.deleteByCustomerNumber(customer.getCustomerNumber());
    addressComponentService.deleteByCustomerNumber(customer.getCustomerNumber());
    
    // Soft delete customer (creates new row with CrudOperation.D)
    customerService.deleteCustomer(customer.getCustomerId());
    
    return ResponseEntity.ok("Deleted successfully");
}
```

---

## Phase 5: Database Migration

Create Flyway migration file (e.g., `V2__add_insert_only_fields.sql`):

```sql
-- Add INSERT-ONLY fields to main entity table
ALTER TABLE customer 
ADD COLUMN crud_operation VARCHAR(1) DEFAULT 'C',
ADD COLUMN version_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add INSERT-ONLY fields to component tables
ALTER TABLE customer_name_component 
ADD COLUMN crud_operation VARCHAR(1) DEFAULT 'C',
ADD COLUMN version_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN customer_number VARCHAR(50);

ALTER TABLE customer_address_component 
ADD COLUMN crud_operation VARCHAR(1) DEFAULT 'C',
ADD COLUMN version_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN customer_number VARCHAR(50);

-- Create indexes for performance
CREATE INDEX idx_customer_number ON customer(customer_number);
CREATE INDEX idx_customer_crud_version ON customer(customer_number, crud_operation, version_timestamp DESC);
CREATE INDEX idx_name_component_customer_number ON customer_name_component(customer_number);
CREATE INDEX idx_address_component_customer_number ON customer_address_component(customer_number);

-- Populate customer_number in existing component rows
UPDATE customer_name_component nc
SET customer_number = (
    SELECT customer_number FROM customer c WHERE c.customer_id = nc.customer_id
);

UPDATE customer_address_component ac
SET customer_number = (
    SELECT customer_number FROM customer c WHERE c.customer_id = ac.customer_id
);

-- Make crud_operation and version_timestamp NOT NULL after populating
ALTER TABLE customer ALTER COLUMN crud_operation SET NOT NULL;
ALTER TABLE customer ALTER COLUMN version_timestamp SET NOT NULL;

ALTER TABLE customer_name_component ALTER COLUMN crud_operation SET NOT NULL;
ALTER TABLE customer_name_component ALTER COLUMN version_timestamp SET NOT NULL;

ALTER TABLE customer_address_component ALTER COLUMN crud_operation SET NOT NULL;
ALTER TABLE customer_address_component ALTER COLUMN version_timestamp SET NOT NULL;
```

---

## Phase 6: Testing Checklist

### 6.1 Create Operations
- [ ] Main entity created with `crudOperation = 'C'`
- [ ] Components created with `crudOperation = 'C'`
- [ ] Components have `customerNumber` set
- [ ] `versionTimestamp` populated for all records

### 6.2 Read Operations
- [ ] GET returns latest non-deleted version
- [ ] GET returns correct component data (queried by customerNumber)
- [ ] Deleted records (crudOperation = 'D') are NOT returned

### 6.3 Update Operations
- [ ] New main entity row created with `crudOperation = 'U'`
- [ ] New component rows created with `crudOperation = 'U'`
- [ ] Original rows remain unchanged (not modified)
- [ ] New rows have new `customerId` but same `customerNumber`
- [ ] Components linked to new customer version via `customerNumber`
- [ ] GET after update returns new version data

### 6.4 Delete Operations
- [ ] New row created with `crudOperation = 'D'`
- [ ] Original rows remain unchanged
- [ ] GET returns 404 after delete (latest version is 'D')

### 6.5 Audit Trail
- [ ] Can retrieve all versions via audit-trail endpoint
- [ ] Versions ordered by `versionTimestamp DESC`
- [ ] Each version shows correct `crudOperation` (C/U/D)

---

## Common Pitfalls & Solutions

### ❌ Pitfall 1: Multiple @PrePersist/@PreUpdate Methods
**Error**: `BeanCreationException: You can only annotate one callback method with jakarta.persistence.PreUpdate`

**Solution**: Each entity can have ONLY ONE `@PrePersist` and ONLY ONE `@PreUpdate` method. Combine logic into single methods.

---

### ❌ Pitfall 2: Updating Existing Objects Instead of Creating New
**Problem**: Original 'C' row gets modified

**Solution**: 
```java
// ❌ WRONG
public Customer update(Customer customer) {
    customer.setCrudOperation(CrudOperation.U);
    return repository.save(customer); // Modifies existing row!
}

// ✅ CORRECT
public Customer update(Customer existing) {
    Customer newVersion = new Customer();
    newVersion.setCustomerNumber(existing.getCustomerNumber());
    // ... copy all fields ...
    newVersion.setCrudOperation(CrudOperation.U);
    return repository.save(newVersion); // Creates new row!
}
```

---

### ❌ Pitfall 3: Components Linked by customerId Instead of customerNumber
**Problem**: After update, GET returns empty components because they're linked to old customerId

**Solution**: Always use business identifier:
```java
// ❌ WRONG
component.setCustomerId(customer.getCustomerId());
List<Component> components = componentService.findByCustomerId(customer.getCustomerId());

// ✅ CORRECT
component.setCustomerNumber(customer.getCustomerNumber());
List<Component> components = componentService.findByCustomerNumber(customer.getCustomerNumber());
```

---

### ❌ Pitfall 4: Repository Methods Don't Filter Deleted Records
**Problem**: GET returns deleted records

**Solution**: Use native queries with `crud_operation != 'D'` filter:
```java
@Query(value = "SELECT * FROM customer WHERE user_id = :userId " +
               "AND crud_operation != 'D' " +
               "ORDER BY version_timestamp DESC LIMIT 1", 
       nativeQuery = true)
Optional<Customer> findLatestByUserId(@Param("userId") String userId);
```

---

### ❌ Pitfall 5: Not Setting CrudOperation/VersionTimestamp on Component Updates
**Problem**: Component updates have `crudOperation = 'C'` or null

**Solution**: Explicitly set on updates:
```java
component.setCrudOperation(Component.CrudOperation.U);
component.setVersionTimestamp(LocalDateTime.now());
component.setCustomerNumber(customer.getCustomerNumber());
```

---

### ❌ Pitfall 6: Forgetting to Set customerNumber on Component Creation
**Problem**: Components created with null customerNumber

**Solution**: Always set during creation:
```java
component.setCustomerNumber(savedCustomer.getCustomerNumber());
```

---

## Quick Reference Checklist

Before deploying, verify:

- [ ] All entities have `crudOperation` and `versionTimestamp` fields
- [ ] Each entity has ONLY ONE `@PrePersist` and ONE `@PreUpdate` method
- [ ] Component entities have `customerNumber` field
- [ ] Repository methods filter out deleted records (`crud_operation != 'D'`)
- [ ] Service update methods create NEW objects (not modify existing)
- [ ] Controller sets `customerNumber` on all component creates/updates
- [ ] Controller sets `crudOperation = U` and `versionTimestamp` on updates
- [ ] Response builders query components by `customerNumber` (not `customerId`)
- [ ] Delete operations use `customerNumber` for component deletion
- [ ] Database migration adds all required columns and indexes
- [ ] All tests pass for create, read, update, delete, and audit trail

---

## Example Implementation Flow

### Step-by-Step Update Flow:
1. User calls `PUT /api/customer/CUST-001` with updated email
2. Controller finds existing customer by customerNumber `CUST-001`
3. Controller updates fields on existing customer object
4. Service creates NEW Customer object with same customerNumber, sets `crudOperation = U`
5. Repository inserts new row (new customerId like `uuid-789`, same customerNumber `CUST-001`)
6. Controller creates NEW component rows with `crudOperation = U`, `customerNumber = CUST-001`
7. Repository inserts new component rows
8. **Result in DB**:
   - Old customer row: `customerId=uuid-123, customerNumber=CUST-001, crudOperation=C`
   - New customer row: `customerId=uuid-789, customerNumber=CUST-001, crudOperation=U`
   - Old component row: `id=comp-456, customerNumber=CUST-001, crudOperation=C`
   - New component row: `id=comp-999, customerNumber=CUST-001, crudOperation=U`
9. GET request returns latest non-deleted version (uuid-789) with components (comp-999)

---

## Support & Resources

For detailed implementation examples, refer to:
- `INSERT_ONLY_FIXES.md` - Original fixes documentation
- `COMPONENT_CUSTOMER_NUMBER_FIXES.md` - Component linking fixes
- Entity classes in `src/main/java/com/nexabank/customer/entity/`
- Repository classes in `src/main/java/com/nexabank/customer/repository/`
- Service classes in `src/main/java/com/nexabank/customer/service/`

---

**Last Updated**: October 24, 2025  
**Status**: Production-Ready Pattern ✅
