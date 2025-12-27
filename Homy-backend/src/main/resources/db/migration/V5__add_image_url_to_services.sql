-- Add image_url column to services table
ALTER TABLE services ADD COLUMN image_url VARCHAR(1024) DEFAULT NULL;

-- Update existing services with fallback image URLs (optional, based on service names)
UPDATE services SET image_url = 'assets/images/cleaning.jpg' WHERE name LIKE '%cleaning%' OR name LIKE '%clean%';
UPDATE services SET image_url = 'assets/images/repair.jpg' WHERE name LIKE '%repair%';
UPDATE services SET image_url = 'assets/images/stitching.jpg' WHERE name LIKE '%stitch%' OR name LIKE '%fabric%';
