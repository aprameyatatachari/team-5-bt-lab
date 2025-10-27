-- Migration to add CRUD operation tracking to component tables
-- This supports the INSERT-ONLY paradigm for audit trail

-- Add CRUD operation fields to customer_name_components
ALTER TABLE customer_name_components 
ADD COLUMN IF NOT EXISTS crud_operation VARCHAR(1) NOT NULL DEFAULT 'C',
ADD COLUMN IF NOT EXISTS version_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add CRUD operation fields to customer_address_components  
ALTER TABLE customer_address_components
ADD COLUMN IF NOT EXISTS crud_operation VARCHAR(1) NOT NULL DEFAULT 'C',
ADD COLUMN IF NOT EXISTS version_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add CRUD operation fields to customer_identification
ALTER TABLE customer_identification
ADD COLUMN IF NOT EXISTS crud_operation VARCHAR(1) NOT NULL DEFAULT 'C', 
ADD COLUMN IF NOT EXISTS version_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add comments to document the purpose
COMMENT ON COLUMN customer_name_components.crud_operation IS 'INSERT-ONLY audit: C=Create, U=Update, D=Delete';
COMMENT ON COLUMN customer_name_components.version_timestamp IS 'Timestamp when this version was created';

COMMENT ON COLUMN customer_address_components.crud_operation IS 'INSERT-ONLY audit: C=Create, U=Update, D=Delete';
COMMENT ON COLUMN customer_address_components.version_timestamp IS 'Timestamp when this version was created';

COMMENT ON COLUMN customer_identification.crud_operation IS 'INSERT-ONLY audit: C=Create, U=Update, D=Delete';
COMMENT ON COLUMN customer_identification.version_timestamp IS 'Timestamp when this version was created';
