package com.homy.backend.repository;

import com.homy.backend.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByBookingId(Long bookingId);
}
