-- Create contacts table to store contact form submissions
CREATE TABLE IF NOT EXISTS contacts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  phone VARCHAR(100),
  message TEXT,
  created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
);
