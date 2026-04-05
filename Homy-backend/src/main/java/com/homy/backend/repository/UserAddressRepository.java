package com.homy.backend.repository;

import com.homy.backend.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    
    // Get all addresses for a user
    List<UserAddress> findByUserIdAndIsActiveTrue(Long userId);
    
    // Get all addresses for a user (including inactive)
    List<UserAddress> findByUserId(Long userId);
    
    // Get default address for a user
    Optional<UserAddress> findByUserIdAndIsDefaultTrueAndIsActiveTrue(Long userId);
    
    // Get address by ID and user ID (security check)
    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);
    
    // Count active addresses for a user
    long countByUserIdAndIsActiveTrue(Long userId);
}
