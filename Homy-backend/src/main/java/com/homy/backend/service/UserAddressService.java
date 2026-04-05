package com.homy.backend.service;

import com.homy.backend.model.UserAddress;
import com.homy.backend.repository.UserAddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserAddressService {

    @Autowired
    private UserAddressRepository userAddressRepository;

    /**
     * Add a new address for user
     */
    public UserAddress addAddress(Long userId, UserAddress address) {
        address.setUserId(userId);
        address.setIsActive(true);
        address.setCreatedAt(LocalDateTime.now());

        // Build full address from components
        StringBuilder fullAddress = new StringBuilder();
        if (address.getHouse() != null && !address.getHouse().trim().isEmpty()) {
            fullAddress.append(address.getHouse()).append(", ");
        }
        if (address.getArea() != null && !address.getArea().trim().isEmpty()) {
            fullAddress.append(address.getArea()).append(", ");
        }
        if (address.getCity() != null && !address.getCity().trim().isEmpty()) {
            fullAddress.append(address.getCity()).append(", ");
        }
        if (address.getPincode() != null && !address.getPincode().trim().isEmpty()) {
            fullAddress.append(address.getPincode());
        }

        address.setFullAddress(fullAddress.toString());

        // If this is the first address, set it as default
        if (userAddressRepository.countByUserIdAndIsActiveTrue(userId) == 0) {
            address.setIsDefault(true);
        }

        return userAddressRepository.save(address);
    }

    /**
     * Get all active addresses for a user
     */
    public List<UserAddress> getUserAddresses(Long userId) {
        return userAddressRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Get address by ID (with user ownership check)
     */
    public Optional<UserAddress> getAddressById(Long addressId, Long userId) {
        return userAddressRepository.findByIdAndUserId(addressId, userId);
    }

    /**
     * Get default address for user
     */
    public Optional<UserAddress> getDefaultAddress(Long userId) {
        return userAddressRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId);
    }

    /**
     * Update an address
     */
    public UserAddress updateAddress(Long addressId, Long userId, UserAddress updatedAddress) {
        Optional<UserAddress> existingOpt = userAddressRepository.findByIdAndUserId(addressId, userId);
        
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("Address not found");
        }

        UserAddress existing = existingOpt.get();
        existing.setLabel(updatedAddress.getLabel());
        existing.setAddressType(updatedAddress.getAddressType());
        existing.setHouse(updatedAddress.getHouse());
        existing.setArea(updatedAddress.getArea());
        existing.setCity(updatedAddress.getCity());
        existing.setPincode(updatedAddress.getPincode());
        existing.setLandmark(updatedAddress.getLandmark());
        existing.setLatitude(updatedAddress.getLatitude());
        existing.setLongitude(updatedAddress.getLongitude());
        existing.setUpdatedAt(LocalDateTime.now());

        // Rebuild full address
        StringBuilder fullAddress = new StringBuilder();
        if (existing.getHouse() != null && !existing.getHouse().trim().isEmpty()) {
            fullAddress.append(existing.getHouse()).append(", ");
        }
        if (existing.getArea() != null && !existing.getArea().trim().isEmpty()) {
            fullAddress.append(existing.getArea()).append(", ");
        }
        if (existing.getCity() != null && !existing.getCity().trim().isEmpty()) {
            fullAddress.append(existing.getCity()).append(", ");
        }
        if (existing.getPincode() != null && !existing.getPincode().trim().isEmpty()) {
            fullAddress.append(existing.getPincode());
        }
        existing.setFullAddress(fullAddress.toString());

        return userAddressRepository.save(existing);
    }

    /**
     * Set address as default
     */
    public void setDefaultAddress(Long addressId, Long userId) {
        Optional<UserAddress> addressOpt = userAddressRepository.findByIdAndUserId(addressId, userId);
        
        if (addressOpt.isEmpty()) {
            throw new RuntimeException("Address not found");
        }

        // Remove default from all other addresses
        List<UserAddress> allAddresses = userAddressRepository.findByUserId(userId);
        for (UserAddress addr : allAddresses) {
            if (!addr.getId().equals(addressId)) {
                addr.setIsDefault(false);
                userAddressRepository.save(addr);
            }
        }

        // Set this as default
        UserAddress address = addressOpt.get();
        address.setIsDefault(true);
        userAddressRepository.save(address);
    }

    /**
     * Delete an address (soft delete)
     */
    public void deleteAddress(Long addressId, Long userId) {
        Optional<UserAddress> addressOpt = userAddressRepository.findByIdAndUserId(addressId, userId);
        
        if (addressOpt.isEmpty()) {
            throw new RuntimeException("Address not found");
        }

        UserAddress address = addressOpt.get();
        address.setIsActive(false);
        address.setUpdatedAt(LocalDateTime.now());
        userAddressRepository.save(address);

        // If this was the default, set the first remaining address as default
        List<UserAddress> remainingAddresses = userAddressRepository.findByUserIdAndIsActiveTrue(userId);
        if (!remainingAddresses.isEmpty() && remainingAddresses.stream().noneMatch(UserAddress::getIsDefault)) {
            remainingAddresses.get(0).setIsDefault(true);
            userAddressRepository.save(remainingAddresses.get(0));
        }
    }
}
