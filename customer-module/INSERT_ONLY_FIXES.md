# INSERT-ONLY Paradigm Fixes

## Issues Fixed

### 1. **Customer Entity Updates Were Modifying Original Records**
**Problem**: When updating a customer, the original 'C' record was being modified instead of creating a new 'U' record.

**Root Cause**: The JPA repository's `save()` method was being called on the existing entity, which updates it in place.

**Solution**: Modified `CustomerService.updateCustomer()` to create a completely new Customer object with a new UUID, copying all fields from the existing customer, and setting `crudOperation = 'U'`.

### 2. **Component Entities Missing CRUD Operation Fields**
**Problem**: `CustomerNameComponent`, `CustomerAddressComponent`, and `CustomerIdentification` entities did not have audit trail fields (`crudOperation`, `versionTimestamp`).

**Solution**: Added to all three component entities:
```java
@Enumerated(EnumType.STRING)
@Column(name = "crud_operation", nullable = false)
private CrudOperation crudOperation = CrudOperation.C;

@Column(name = "version_timestamp", nullable = false)
private LocalDateTime versionTimestamp;

public enum CrudOperation {
    C, // Create
    U, // Update  
    D  // Delete
}
```

Also updated `@PrePersist` to set these fields if null.

### 3. **Component Updates Not Marked as 'U'**
**Problem**: When updating customer profiles, new component records were created with `crudOperation = 'C'` instead of 'U'.

**Solution**: Explicitly set `crudOperation = 'U'` and `versionTimestamp` when creating component records during update operations in:
- `updateAddress()` - address components
- `updateName()` - name components
- `updateIdentification()` - identification documents

### 4. **GET Queries Not Filtering Out Deleted Records**
**Problem**: Queries were returning records even if they were marked as 'D' (deleted).

**Solution**: Updated ALL repository query methods to filter out deleted records:

#### Single Record Queries (use native SQL with LIMIT 1):
```java
@Query(value = "SELECT * FROM customers WHERE customer_number = :customerNumber " +
       "AND crud_operation != 'D' " +
       "ORDER BY version_timestamp DESC LIMIT 1", nativeQuery = true)
Optional<Customer> findLatestByCustomerNumber(@Param("customerNumber") String customerNumber);

@Query(value = "SELECT * FROM customers WHERE user_id = :userId " +
       "AND crud_operation != 'D' " +
       "ORDER BY version_timestamp DESC LIMIT 1", nativeQuery = true)
Optional<Customer> findLatestByUserId(@Param("userId") String userId);

@Query(value = "SELECT * FROM customers WHERE email_id = :emailId " +
       "AND crud_operation != 'D' " +
       "ORDER BY version_timestamp DESC LIMIT 1", nativeQuery = true)
Optional<Customer> findLatestByEmail(@Param("emailId") String emailId);
```

#### List Queries (filter and get latest versions):
```java
@Query("SELECT c FROM Customer c WHERE c.customerStatus = :status AND c.crudOperation != 'D' " +
       "AND c.customerId IN (SELECT MAX(c2.customerId) FROM Customer c2 WHERE c2.customerNumber = c.customerNumber)")
List<Customer> findByCustomerStatus(@Param("status") Customer.CustomerStatus status);
```

Applied to:
- `findByCustomerStatus()`
- `findByCustomerType()`
- `findByKycStatus()`
- `findByNameContainingIgnoreCase()`
- `findByPhoneNumberContaining()`
- `findAllActiveCustomers()`
- `findCustomersRequiringKyc()`

#### Exists/Count Queries:
```java
@Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.userId = :userId AND c.crudOperation != 'D'")
boolean existsByUserId(@Param("userId") String userId);
```

Applied to:
- `existsByUserId()`
- `existsByEmailId()`
- `existsByAadharNumber()`
- `existsByPanNumber()`
- `countByCustomerStatus()`

This ensures:
- Only non-deleted records are returned
- Latest version is returned for queries
- Proper ordering by version timestamp

## Database Schema Updates Needed

Run these SQL scripts to add the new columns to component tables:

```sql
-- Add CRUD operation fields to customer_name_components
ALTER TABLE customer_name_components 
ADD COLUMN crud_operation VARCHAR(1) NOT NULL DEFAULT 'C',
ADD COLUMN version_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add CRUD operation fields to customer_address_components  
ALTER TABLE customer_address_components
ADD COLUMN crud_operation VARCHAR(1) NOT NULL DEFAULT 'C',
ADD COLUMN version_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add CRUD operation fields to customer_identification
ALTER TABLE customer_identification
ADD COLUMN crud_operation VARCHAR(1) NOT NULL DEFAULT 'C', 
ADD COLUMN version_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
```

## INSERT-ONLY Behavior Now Working

### On CREATE:
- Customer record created with `crudOperation = 'C'`
- All component records created with `crudOperation = 'C'`

### On UPDATE:
- New Customer record created with `crudOperation = 'U'` and new UUID
- Old Customer record remains unchanged with `crudOperation = 'C'`
- New component records created with `crudOperation = 'U'`
- Old component records remain unchanged

### On DELETE:
- New Customer record created with `crudOperation = 'D'` and new UUID
- Old records remain unchanged
- `customerStatus` set to 'CLOSED'

### On GET:
- Returns only latest record where `crudOperation != 'D'`
- Uses LIMIT 1 to ensure single record
- Orders by `version_timestamp DESC`

## Audit Trail

All versions of a customer and their components are preserved with:
- `crudOperation`: 'C', 'U', or 'D'
- `versionTimestamp`: When the version was created
- `customerNumber`: Business identifier that stays the same across versions
- `customerId`: Unique UUID for each version (technical key)

Use the `/api/profiles/user/{userId}/audit-trail` endpoint to view all versions.
