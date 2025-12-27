-- Migration: convert bookings.id from varchar to BIGINT AUTO_INCREMENT, add reference
-- WARNING: run on a backup/test DB first. This script assumes MySQL.
START TRANSACTION;

-- Add new auto-increment column
ALTER TABLE bookings ADD COLUMN id_new BIGINT NOT NULL AUTO_INCREMENT FIRST;

-- Drop old primary key (assuming 'id' is current PK)
ALTER TABLE bookings DROP PRIMARY KEY;

-- Set id_new as primary key
ALTER TABLE bookings ADD PRIMARY KEY (id_new);

-- Rename old id to legacy_id to preserve original values
ALTER TABLE bookings CHANGE COLUMN id legacy_id VARCHAR(255);

-- Rename id_new to id (make it the canonical PK)
ALTER TABLE bookings CHANGE COLUMN id_new id BIGINT NOT NULL AUTO_INCREMENT;

-- Add reference column if not present
ALTER TABLE bookings ADD COLUMN `reference` VARCHAR(255);

-- Populate reference as HOMY{YEAR}{id padded to 6 digits}
UPDATE bookings SET `reference` = CONCAT('HOMY', YEAR(COALESCE(created_at, NOW())), LPAD(id,6,'0'));

COMMIT;

-- Note: After running, consider removing legacy_id or keeping it for traceability.
