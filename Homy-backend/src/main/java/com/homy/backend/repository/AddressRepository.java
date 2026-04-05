package com.homy.backend.repository;

import com.homy.backend.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByBookingId(Long bookingId);
    Optional<Address> findTopByCustomerIdOrderByCreatedAtDesc(Long customerId);
    
    // Batch query to get all addresses for multiple booking IDs (avoids N+1 queries)
    @Query("SELECT a FROM Address a WHERE a.bookingId IN :bookingIds")
    List<Address> findByBookingIds(@Param("bookingIds") List<Long> bookingIds);
    
    // Batch query to get most recent address for multiple customer IDs
    @Query("SELECT a FROM Address a WHERE a.customerId = :customerId ORDER BY a.createdAt DESC LIMIT 1")
    Optional<Address> findLatestByCustomerId(@Param("customerId") Long customerId);
}
