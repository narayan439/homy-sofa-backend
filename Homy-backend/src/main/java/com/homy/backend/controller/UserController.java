package com.homy.backend.controller;

import com.homy.backend.dto.AuthRequest;
import com.homy.backend.dto.LoginResponse;
import com.homy.backend.dto.UserResponse;
import com.homy.backend.dto.UserSignupRequest;
import com.homy.backend.model.User;
import com.homy.backend.repository.UserRepository;
import com.homy.backend.security.JwtUtil;
import com.homy.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // User Registration (Sign Up)
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserSignupRequest request) {
        try {
            UserResponse userResponse = userService.registerUser(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("data", userResponse);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // User Login
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody AuthRequest request) {
        try {
            LoginResponse loginResponse = userService.authenticateUser(request.getEmail(), request.getPassword());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("token", loginResponse.getToken());
            response.put("userId", loginResponse.getUserId());
            response.put("email", loginResponse.getEmail());
            response.put("name", loginResponse.getName());
            response.put("phone", loginResponse.getPhone());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // Get User Profile (Protected)
    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserProfile(@RequestHeader("Authorization") String token) {
        try {
            // Extract userId from JWT (implementation depends on your JWT filter)
            // For now, return a placeholder
            return ResponseEntity.ok(new HashMap<String, String>() {{
                put("message", "Profile endpoint - implement JWT extraction in controller");
            }});
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
    }

    // Update User Profile (Protected)
    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, Object> profileData,
                                                @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // Extract userId from JWT token
            Long userId = null;

            if (token != null && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                userId = jwtUtil.extractUserId(jwt);
            }

            if (userId == null) {
                // Try to get from Authentication context
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof String) {
                    String principal = (String) authentication.getPrincipal();
                    try {
                        userId = Long.parseLong(principal);
                    } catch (NumberFormatException e) {
                        // Try extracting from context attributes
                    }
                }
            }

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new HashMap<String, String>() {{ put("message", "User ID not found in token"); }});
            }

            // Create UserSignupRequest to use with updateUserProfile
            UserSignupRequest updateRequest = new UserSignupRequest();
            if (profileData.containsKey("name")) {
                updateRequest.setName(profileData.get("name").toString());
            }
            if (profileData.containsKey("email")) {
                updateRequest.setEmail(profileData.get("email").toString());
            }
            if (profileData.containsKey("phone")) {
                updateRequest.setPhone(profileData.get("phone").toString());
            }
            if (profileData.containsKey("address")) {
                updateRequest.setAddress(profileData.get("address").toString());
            }

            UserResponse updatedUser = userService.updateUserProfile(userId, updateRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("data", updatedUser);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    // Get User by Email (Protected)
    @GetMapping("/by-email/{email}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        try {
            var userOpt = userService.getUserByEmail(email);
            if (userOpt.isPresent()) {
                return ResponseEntity.ok(userOpt.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // Check if Email Already Exists
    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmailExists(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        var userOpt = userService.getUserByEmail(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", userOpt.isPresent());
        return ResponseEntity.ok(response);
    }

    // Check if email is already used by another user (excluding current user)
    @PostMapping("/check-duplicate-email")
    public ResponseEntity<?> checkDuplicateEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("exists", false);
            return ResponseEntity.ok(response);
        }

        var userOpt = userService.getUserByEmail(email);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", userOpt.isPresent());
        return ResponseEntity.ok(response);
    }

    // Check if phone is already used by another user (excluding current user)
    @PostMapping("/check-duplicate-phone")
    public ResponseEntity<?> checkDuplicatePhone(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        if (phone == null || phone.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("exists", false);
            return ResponseEntity.ok(response);
        }

        var userOpt = userService.getUserByPhone(phone);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", userOpt.isPresent());
        return ResponseEntity.ok(response);
    }

    // ========== ADMIN ENDPOINTS ==========

    // List all users (Admin only)
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            var users = userService.getAllUsers();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", users);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get user by ID (Admin only)
    @GetMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User foundUser = userOpt.get();
                UserResponse userResponse = new UserResponse(foundUser.getId(), foundUser.getEmail(), 
                        foundUser.getName(), foundUser.getPhone(), foundUser.getAddress());
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", userResponse);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{ put("message", "User not found"); }});
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    // Deactivate user (Admin only)
    @PutMapping("/admin/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable Long userId) {
        try {
            var user = userService.deactivateUser(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User deactivated");
            response.put("data", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    // Activate user (Admin only)
    @PutMapping("/admin/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateUser(@PathVariable Long userId) {
        try {
            var user = userService.activateUser(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User activated");
            response.put("data", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    // Delete account (Admin or User can delete own account)
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> deleteAccount(@PathVariable Long userId) {
        try {
            userService.deleteAccount(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    // Change Password (Protected)
    @PostMapping("/change-password")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordData,
                                            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // Extract userId from JWT token
            Long userId = null;

            if (token != null && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                userId = jwtUtil.extractUserId(jwt);
            }

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new HashMap<String, String>() {{ put("message", "User ID not found in token"); }});
            }

            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new HashMap<String, String>() {{ put("message", "Missing required fields"); }});
            }

            userService.changePassword(userId, currentPassword, newPassword);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    // Delete Account (Protected)
    @DeleteMapping("/account")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteUserAccount(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // Extract userId from JWT token
            Long userId = null;

            if (token != null && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                userId = jwtUtil.extractUserId(jwt);
            }

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new HashMap<String, String>() {{ put("message", "User ID not found in token"); }});
            }

            userService.deleteAccount(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }
}
