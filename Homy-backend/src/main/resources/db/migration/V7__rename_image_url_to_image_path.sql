-- Rename image_url to image_path and normalize values
ALTER TABLE services CHANGE COLUMN image_url image_path VARCHAR(1024) DEFAULT NULL;

-- Remove any leading slashes so stored values look like "assets/backend/cleaning.jpg"
UPDATE services SET image_path = TRIM(LEADING '/' FROM image_path) WHERE image_path IS NOT NULL;
