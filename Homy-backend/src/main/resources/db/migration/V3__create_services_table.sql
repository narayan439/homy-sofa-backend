-- Create services table
CREATE TABLE IF NOT EXISTS services (
  id VARCHAR(255) NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(255),
  price DOUBLE,
  is_active BIT(1) DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_active (is_active)
);

-- Insert default services (optional)
INSERT INTO services (id, name, description, price, is_active) VALUES
('cleaning-001', 'Cleaning Service', 'Full house cleaning', 5000.0, 1),
('maintenance-001', 'Maintenance', 'Furniture maintenance', 3000.0, 1),
('repair-001', 'Repair Service', 'Furniture repair', 4000.0, 1),
('upholstery-001', 'Upholstery', 'Upholstery cleaning and repair', 6000.0, 1)
ON DUPLICATE KEY UPDATE id=id;
