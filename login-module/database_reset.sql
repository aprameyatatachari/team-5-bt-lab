-- Clear and recreate nexabank_auth database
DROP DATABASE IF EXISTS nexabank_auth;
CREATE DATABASE nexabank_auth;
USE nexabank_auth;

-- Create auth_users table with enhanced structure
CREATE TABLE auth_users (
    user_id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_type ENUM('CUSTOMER', 'ADMIN', 'EMPLOYEE', 'SYSTEM') NOT NULL DEFAULT 'CUSTOMER',
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED', 'SUSPENDED', 'PENDING_ACTIVATION') NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INT DEFAULT 0,
    last_login DATETIME,
    account_locked_until DATETIME,
    password_changed_at DATETIME,
    must_change_password BOOLEAN DEFAULT FALSE,
    
    -- Audit fields from AuditLoggable
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    CRUD_VALUE ENUM('CREATE', 'READ', 'UPDATE', 'DELETE'),
    AUDIT_USER_ID VARCHAR(255),
    WS_ID VARCHAR(255),
    PRGM_ID VARCHAR(255),
    HOST_TS TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    LOCAL_TS TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ACPT_TS TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ACPT_TS_UTC_OFST TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UUID_REFERENCE VARCHAR(36)
);

-- Create user_roles table for role mappings
CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL,
    role ENUM(
        'CUSTOMER_VIEW', 'CUSTOMER_TRANSACTION',
        'EMPLOYEE_VIEW', 'EMPLOYEE_CUSTOMER_MANAGEMENT',
        'ADMIN_VIEW', 'ADMIN_USER_MANAGEMENT', 'ADMIN_SYSTEM_CONFIG', 
        'ADMIN_REPORTS', 'ADMIN_FULL_ACCESS',
        'SYSTEM_API_ACCESS'
    ) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES auth_users(user_id) ON DELETE CASCADE
);

-- Create preset admin account
SET @admin_id = UUID();
SET @admin_password = '$2a$10$YCWWqO0KZcT6Rn1g9K1Kae7ZvJhJX8QY3x6RCN1K7S8Wqz9yBvC0G'; -- BCrypt hash for "admin123"

INSERT INTO auth_users (
    user_id, email, password_hash, user_type, status, 
    password_changed_at, must_change_password, CREATED_AT, CRUD_VALUE
) VALUES (
    @admin_id, 'admin@nexabank.com', @admin_password, 'ADMIN', 'ACTIVE',
    NOW(), FALSE, NOW(), 'CREATE'
);

-- Grant all admin roles to the preset admin account
INSERT INTO user_roles (user_id, role) VALUES
(@admin_id, 'ADMIN_FULL_ACCESS'),
(@admin_id, 'ADMIN_USER_MANAGEMENT'),
(@admin_id, 'ADMIN_SYSTEM_CONFIG'),
(@admin_id, 'ADMIN_REPORTS'),
(@admin_id, 'ADMIN_VIEW');

-- Display the created admin account details (for reference)
SELECT 
    user_id,
    email,
    user_type,
    status,
    'admin123' as password_note,
    CREATED_AT
FROM auth_users 
WHERE email = 'admin@nexabank.com';

SELECT 
    au.email,
    ur.role
FROM auth_users au
JOIN user_roles ur ON au.user_id = ur.user_id
WHERE au.email = 'admin@nexabank.com';

COMMIT;