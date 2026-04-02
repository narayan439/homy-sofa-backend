-- Add price column to bookings table
ALTER TABLE bookings ADD COLUMN price DOUBLE DEFAULT NULL;
