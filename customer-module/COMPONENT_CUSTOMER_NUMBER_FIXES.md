# Component-Customer Number Linkage Fixes

## Problem Statement

Components (CustomerNameComponent, CustomerAddressComponent, CustomerIdentification) were linked to the Customer entity using the `customerId` field (UUID primary key), which changes with each INSERT-ONLY version of the customer. This caused components to remain associated with old customer versions when a new customer version was created during an update operation.

## Solution

Added `customerNumber` field to all component entities to link them to the stable business identifier instead of the mutable technical primary key.

## Changes Made

### 1. Entity Updates

Added to **CustomerNameComponent.java**, **CustomerAddressComponent.java**, and **CustomerIdentification.java**:
```java
@Column(name = "customer_number")
private String customerNumber;
```

### 2. Repository Updates

Added to component repositories:
```java
// Find components by customer number (business ID) - returns latest non-deleted versions
List<ComponentEntity> findByCustomerNumberOrderByVersionDesc(String customerNumber);

// Delete all versions of components for a customer number
void deleteByCustomerNumber(String customerNumber);
```

### 3. Service Layer Updates

Updated component services to expose:
```java
public List<ComponentEntity> findByCustomerNumber(String customerNumber);
public void deleteByCustomerNumber(String customerNumber);
```

### 4. Controller Updates

**createProfile()** - Set customerNumber when creating initial components:
```java
component.setCustomerNumber(savedCustomer.getCustomerNumber());
```

**update endpoints** - Set customerNumber on all new component versions:
- updateAddress()
- updateName()
- updateIdentification()
- updateProfileByCustomerNumber()

All now include:
```java
component.setCustomerNumber(updatedCustomer.getCustomerNumber());
component.setCrudOperation(ComponentEntity.CrudOperation.U);
component.setVersionTimestamp(LocalDateTime.now());
```

**deleteProfile()** - Use customerNumber for stable reference:
```java
nameComponentService.deleteByCustomerNumber(customer.getCustomerNumber());
identificationService.deleteByCustomerNumber(customer.getCustomerNumber());
addressComponentService.deleteByCustomerNumber(customer.getCustomerNumber());
```

**createUserProfileResponse()** - Query components by customerNumber:
```java
List<CustomerNameComponent> nameComponents = nameComponentService.findByCustomerNumber(customer.getCustomerNumber());
List<CustomerAddressComponent> addressComponents = addressComponentService.findByCustomerNumber(customer.getCustomerNumber());
List<CustomerIdentification> identifications = identificationService.findByCustomerNumber(customer.getCustomerNumber());
```

## Database Migration

Run the following migration to add `customer_number` column to component tables:

```sql
-- Add customer_number column to component tables
ALTER TABLE customer_name_component 
ADD COLUMN customer_number VARCHAR(50);

ALTER TABLE customer_address_component 
ADD COLUMN customer_number VARCHAR(50);

ALTER TABLE customer_identification 
ADD COLUMN customer_number VARCHAR(50);

-- Add indexes for performance
CREATE INDEX idx_name_component_customer_number ON customer_name_component(customer_number);
CREATE INDEX idx_address_component_customer_number ON customer_address_component(customer_number);
CREATE INDEX idx_identification_customer_number ON customer_identification(customer_number);

-- For existing data, populate customer_number from customer table
UPDATE customer_name_component nc
SET customer_number = (
    SELECT customer_number FROM customer c WHERE c.customer_id = nc.customer_id
);

UPDATE customer_address_component ac
SET customer_number = (
    SELECT customer_number FROM customer c WHERE c.customer_id = ac.customer_id
);

UPDATE customer_identification ci
SET customer_number = (
    SELECT customer_number FROM customer c WHERE c.customer_id = ci.customer_id
);
```

## Benefits

1. **Stable Linkage**: Components remain correctly associated across customer version updates
2. **Audit Trail**: Component versions can be traced through time using customerNumber
3. **Query Performance**: Latest components for a customer can be retrieved using customerNumber
4. **Data Integrity**: Business identifier ensures consistent reference regardless of technical PK changes

## Verification Steps

1. Run database migration
2. Create a customer profile
3. Update customer name/address/identification
4. Verify GET endpoint returns correct latest component data
5. Check audit trail to ensure all component versions are preserved
6. Confirm components use customerNumber in database

## Notes

- Components created during initial profile creation have `crudOperation = 'C'`
- Components created during updates have `crudOperation = 'U'`
- `versionTimestamp` tracks when each component version was created
- Both `customerId` (FK) and `customerNumber` (business ID) are maintained for flexibility
- Read operations should always use `findByCustomerNumber()` to get latest components
- Delete operations should use `deleteByCustomerNumber()` to remove all versions
