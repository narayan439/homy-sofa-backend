package com.homy.backend.controller;

import com.homy.backend.model.UserAddress;
import com.homy.backend.security.JwtUtil;
import com.homy.backend.service.UserAddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin(origins = "*")
public class UserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Get all addresses for current user
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserAddresses(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            List<UserAddress> addresses = userAddressService.getUserAddresses(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", addresses);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    /**
     * Add new address
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addAddress(
            @Valid @RequestBody UserAddress address,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            UserAddress savedAddress = userAddressService.addAddress(userId, address);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Address added successfully");
            response.put("data", savedAddress);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    /**
     * Get single address by ID
     */
    @GetMapping("/{addressId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getAddress(
            @PathVariable Long addressId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            var addressOpt = userAddressService.getAddressById(addressId, userId);
            
            if (addressOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{ put("message", "Address not found"); }});
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", addressOpt.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    /**
     * Update address
     */
    @PutMapping("/{addressId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody UserAddress updatedAddress,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            UserAddress address = userAddressService.updateAddress(addressId, userId, updatedAddress);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Address updated successfully");
            response.put("data", address);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    /**
     * Set address as default
     */
    @PutMapping("/{addressId}/set-default")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> setDefaultAddress(
            @PathVariable Long addressId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            userAddressService.setDefaultAddress(addressId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Default address updated");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    /**
     * Delete address
     */
    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteAddress(
            @PathVariable Long addressId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            userAddressService.deleteAddress(addressId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Address deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    /**
     * Get default address
     */
    @GetMapping("/default")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getDefaultAddress(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = extractUserIdFromToken(token);
            var addressOpt = userAddressService.getDefaultAddress(userId);
            
            Map<String, Object> response = new HashMap<>();
            if (addressOpt.isPresent()) {
                response.put("success", true);
                response.put("data", addressOpt.get());
            } else {
                response.put("success", false);
                response.put("message", "No default address set");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    /**
     * Extract user ID from JWT token
     */
    private Long extractUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid token");
        }
        String jwt = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwt);
        if (userId == null) {
            throw new RuntimeException("User ID not found in token");
        }
        return userId;
    }
}
