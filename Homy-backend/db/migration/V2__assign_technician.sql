-- Migration: add technician-related columns if they don't exist
ALTER TABLE bookings
  ADD COLUMN IF NOT EXISTS technician_id BIGINT,
  ADD COLUMN IF NOT EXISTS technician_status VARCHAR(64),
  ADD COLUMN IF NOT EXISTS technician_notes TEXT,
  ADD COLUMN IF NOT EXISTS additional_services_json LONGTEXT,
  ADD COLUMN IF NOT EXISTS total_amount DOUBLE;

-- Note: Your project may not run Flyway automatically. Run this SQL against your DB if needed.
