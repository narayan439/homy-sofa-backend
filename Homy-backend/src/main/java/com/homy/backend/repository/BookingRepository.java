package com.homy.backend.repository;

import com.homy.backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStatus(String status);
    List<Booking> findByEmail(String email);
    List<Booking> findByCustomerId(Long customerId);
    long countByCustomerId(Long customerId);
    
    // Check if customer has an active booking for the same service (PENDING or APPROVED)
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b WHERE b.customerId = :customerId AND b.service = :service AND b.status IN ('PENDING', 'APPROVED')")
    boolean hasActiveBookingForService(@Param("customerId") Long customerId, @Param("service") String service);

    // Get active booking for service (used to return reference number to frontend)
    @Query("SELECT b FROM Booking b WHERE b.customerId = :customerId AND b.service = :service AND b.status IN ('PENDING', 'APPROVED') ORDER BY b.createdAt DESC LIMIT 1")
    Booking findActiveBookingForService(@Param("customerId") Long customerId, @Param("service") String service);
}
