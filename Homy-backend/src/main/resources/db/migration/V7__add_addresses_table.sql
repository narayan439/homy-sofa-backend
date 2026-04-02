-- Create addresses table to store booking/customer addresses and lat/long
CREATE TABLE IF NOT EXISTS `addresses` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `booking_id` BIGINT NOT NULL,
  `customer_id` BIGINT DEFAULT NULL,
  `address_text` VARCHAR(2048) DEFAULT NULL,
  `lat_long` VARCHAR(128) DEFAULT NULL,
  `created_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX (`booking_id`),
  CONSTRAINT `fk_addresses_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
