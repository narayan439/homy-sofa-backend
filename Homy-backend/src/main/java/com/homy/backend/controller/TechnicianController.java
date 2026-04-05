package com.homy.backend.controller;

import com.homy.backend.model.Technician;
import com.homy.backend.service.TechnicianService;
import com.homy.backend.repository.TechnicianRepository;
import com.homy.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@CrossOrigin(origins = "*")
public class TechnicianController {

    @Autowired
    private TechnicianService technicianService;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private com.homy.backend.repository.BookingRepository bookingRepository;

    @Autowired
    private com.homy.backend.service.AssignmentService assignmentService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // Admin APIs
    @PostMapping("/api/admin/technicians")
    public ResponseEntity<?> createTechnician(@RequestBody Technician t) {
        // validate unique email and phone
        if (t.getEmail() != null && technicianRepository.findByEmail(t.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already in use"));
        }
        if (t.getPhone() != null && technicianRepository.findByPhone(t.getPhone()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone already in use"));
        }
        if (t.getPassword() != null) t.setPassword(passwordEncoder.encode(t.getPassword()));
        Technician saved = technicianService.createTechnician(t);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/api/admin/technicians")
    public ResponseEntity<?> getTechnicians() {
        List<Technician> all = technicianService.getAllTechnicians();
        return ResponseEntity.ok(all);
    }

    @PutMapping("/api/admin/technicians/{id}")
    public ResponseEntity<?> updateTechnician(@PathVariable Long id, @RequestBody Technician t) {
        // check email conflict
        if (t.getEmail() != null) {
            var byEmail = technicianRepository.findByEmail(t.getEmail());
            if (byEmail.isPresent() && !byEmail.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already in use by another technician"));
            }
        }
        // check phone conflict
        if (t.getPhone() != null) {
            var byPhone = technicianRepository.findByPhone(t.getPhone());
            if (byPhone.isPresent() && !byPhone.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Phone already in use by another technician"));
            }
        }
        // encode password if provided
        if (t.getPassword() != null && !t.getPassword().isEmpty()) {
            t.setPassword(passwordEncoder.encode(t.getPassword()));
        }
        Technician updated = technicianService.updateTechnician(id, t);
        if (updated == null) return ResponseEntity.status(404).body(Map.of("error", "Technician not found"));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/api/admin/technicians/{id}")
    public ResponseEntity<?> deleteTechnician(@PathVariable Long id) {
        boolean ok = technicianService.deleteTechnician(id);
        if (!ok) return ResponseEntity.status(404).body("Technician not found");
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/api/admin/bookings/{bookingId}/assign")
    public ResponseEntity<?> assignTechnician(@PathVariable Long bookingId) {
        // Automatic assignment: choose best available technician for this booking
        return bookingRepository.findById(bookingId).map(b -> {
            try {
                var assigned = assignmentService.assignTechnicianForBooking(b);
                if (assigned.isPresent()) {
                    return ResponseEntity.ok(Map.of("success", true, "technicianId", assigned.get().getId(), "technicianName", assigned.get().getName()));
                } else {
                    return ResponseEntity.status(404).body(Map.of("error", "No available technician found"));
                }
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of("error", "Assignment failed"));
            }
        }).orElse(ResponseEntity.status(404).body(Map.of("error", "Booking not found")));
    }

    // Technician APIs
    @PostMapping("/api/technician/login")
    public ResponseEntity<?> technicianLogin(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String password = req.get("password");
        return technicianRepository.findByEmail(email).map(t -> {
            if (passwordEncoder.matches(password, t.getPassword())) {
                String token = jwtUtil.generateToken(t.getId().toString(), t.getEmail(), "TECHNICIAN");
                Map<String, Object> res = new HashMap<>();
                res.put("token", token);
                res.put("id", t.getId());
                res.put("email", t.getEmail());
                res.put("name", t.getName());
                return ResponseEntity.ok(res);
            }
            return ResponseEntity.status(401).build();
        }).orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/api/admin/technicians/{id}/reset-password")
    public ResponseEntity<?> resetTechnicianPassword(@PathVariable Long id) {
        var maybe = technicianRepository.findById(id);
        if (maybe.isEmpty()) return ResponseEntity.status(404).body("Technician not found");
        Technician t = maybe.get();
        // Generate default password: first 4 letters of name + first 3 digits of phone (fallbacks)
        String name = t.getName() != null ? t.getName().replaceAll("\\s+", "") : "user";
        String phone = t.getPhone() != null ? t.getPhone().replaceAll("\\D", "") : "000";
        String p1 = name.length() >= 4 ? name.substring(0,4) : name;
        String p2 = phone.length() >= 3 ? phone.substring(0,3) : phone;
        String raw = (p1 + p2);
        if (raw.length() < 4) raw = raw + "000";
        String encoded = passwordEncoder.encode(raw);
        boolean ok = technicianService.setTechnicianPassword(id, encoded);
        if (!ok) return ResponseEntity.status(500).body("Could not reset password");
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("password", raw);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/api/admin/technicians/{id}")
    public ResponseEntity<?> getTechnicianById(@PathVariable Long id) {
        var maybe = technicianRepository.findById(id);
        if (maybe.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Technician not found"));
        return ResponseEntity.ok(maybe.get());
    }

    @PatchMapping("/api/admin/technicians/{id}/status")
    public ResponseEntity<?> updateTechnicianStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String status = body.getOrDefault("status", "").trim().toLowerCase();
            
            if (status.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
            }
            
            if (!status.equals("active") && !status.equals("inactive")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status must be 'active' or 'inactive'"));
            }
            
            var maybe = technicianRepository.findById(id);
            if (maybe.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Technician not found"));
            }
            
            Technician t = maybe.get();
            boolean active = "active".equals(status);
            t.setIsActive(active);
            
            Technician updated = technicianRepository.save(t);
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Status updated successfully",
                "technician", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update status: " + e.getMessage()));
        }
    }

    @GetMapping("/api/admin/technicians/{id}/bookings")
    public ResponseEntity<?> getTechnicianBookings(
            @PathVariable Long id,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "100") Integer size) {
        try {
            // Verify technician exists
            var maybe = technicianRepository.findById(id);
            if (maybe.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Technician not found"));
            }
            
            // Get technician's bookings
            var result = technicianService.getBookingsForTechnician(id, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch bookings: " + e.getMessage()));
        }
    }

    @GetMapping("/api/technician/bookings")
    public ResponseEntity<?> getBookingsForTechnician(@RequestHeader(value = "Authorization", required = false) String auth,
                                                      @RequestParam(value = "technicianId", required = false) Long technicianId,
                                                      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                      @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        // If technicianId provided as param, use it; otherwise try to extract from token
        Long tid = technicianId;
        if (tid == null && auth != null && auth.startsWith("Bearer ")) {
            var claims = jwtUtil.validateToken(auth.substring(7));
            String sub = claims.getSubject();
            try { tid = Long.valueOf(sub); } catch (Exception e) { }
        }
        if (tid == null) return ResponseEntity.badRequest().body("technicianId required");
        // Use paginated endpoint by default
        try {
            var result = technicianService.getBookingsForTechnician(tid, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Fallback to non-paginated list if anything fails
            var bookings = technicianService.getBookingsForTechnician(tid);
            return ResponseEntity.ok(Map.of("total", bookings.size(), "page", 0, "size", bookings.size(), "bookings", bookings));
        }
    }

    @PutMapping("/api/technician/bookings/{id}/accept")
    public ResponseEntity<?> acceptJob(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long techId = null;
        if (body != null && body.containsKey("technicianId")) {
            try { techId = Long.valueOf(String.valueOf(body.get("technicianId"))); } catch (Exception e) { }
        }
        boolean ok = technicianService.acceptJob(id, techId);
        if (ok) return ResponseEntity.ok(Map.of("success", true));
        return ResponseEntity.status(404).body("Booking not found, technician mismatch, or invalid status transition");
    }

    @PostMapping("/api/technician/bookings/{id}/additional-service")
    public ResponseEntity<?> addAdditionalService(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long techId = null;
        if (body != null && body.containsKey("technicianId")) {
            try { techId = Long.valueOf(String.valueOf(body.get("technicianId"))); } catch (Exception e) { }
        }
        
        String serviceName = body != null ? String.valueOf(body.get("serviceName")) : "";
        Double price = 0.0;
        if (body != null && body.containsKey("price")) {
            try { price = Double.valueOf(String.valueOf(body.get("price"))); } catch (Exception e) { }
        }
        
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "serviceName is required"));
        }
        
        try {
            boolean ok = technicianService.addAdditionalService(id, techId, serviceName, price);
            if (ok) return ResponseEntity.ok(Map.of("success", true));
            return ResponseEntity.status(404).body("Booking not found or technician mismatch");
        } catch (IllegalArgumentException iae) {
            // duplicate main service or duplicate extra
            return ResponseEntity.status(409).body(Map.of("error", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to add service"));
        }
    }

    @PutMapping("/api/technician/bookings/{id}/start")
    public ResponseEntity<?> startJob(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long techId = null;
        if (body != null && body.containsKey("technicianId")) {
            try { techId = Long.valueOf(String.valueOf(body.get("technicianId"))); } catch (Exception e) { }
        }
        boolean ok = technicianService.startJob(id, techId);
        if (ok) return ResponseEntity.ok(Map.of("success", true));
        return ResponseEntity.status(404).body("Booking not found or technician mismatch");
    }

    @PutMapping("/api/technician/bookings/{id}/complete")
    public ResponseEntity<?> completeJob(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long techId = null;
        if (body != null && body.containsKey("technicianId")) {
            try { techId = Long.valueOf(String.valueOf(body.get("technicianId"))); } catch (Exception e) { }
        }
        boolean ok = technicianService.completeJob(id, techId, body);
        if (ok) return ResponseEntity.ok(Map.of("success", true));
        return ResponseEntity.status(404).body("Booking not found or technician mismatch");
    }

    @PutMapping("/api/technician/bookings/{id}/cancel")
    public ResponseEntity<?> cancelJob(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long techId = null;
        String reason = null;
        if (body != null) {
            if (body.containsKey("technicianId")) {
                try { techId = Long.valueOf(String.valueOf(body.get("technicianId"))); } catch (Exception e) { }
            }
            if (body.containsKey("cancelReason")) {
                reason = String.valueOf(body.get("cancelReason"));
            }
        }
        boolean ok = technicianService.cancelJob(id, techId, reason);
        if (ok) return ResponseEntity.ok(Map.of("success", true));
        return ResponseEntity.status(404).body("Booking not found or technician mismatch");
    }

    /**
     * Get current technician's profile from JWT token
     */
    @GetMapping("/api/technician/profile")
    public ResponseEntity<?> getTechnicianProfile(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Authorization header required"));
        }
        
        try {
            var claims = jwtUtil.validateToken(auth.substring(7));
            String sub = claims.getSubject();
            Long technicianId = Long.valueOf(sub);
            var maybe = technicianRepository.findById(technicianId);
            if (maybe.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Technician not found"));
            }
            return ResponseEntity.ok(maybe.get());
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }

    /**
     * Update current technician's profile from JWT token
     */
    @PutMapping("/api/technician/profile")
    public ResponseEntity<?> updateTechnicianProfile(@RequestHeader(value = "Authorization", required = false) String auth,
                                                     @RequestBody Technician updatedTech) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Authorization header required"));
        }
        
        try {
            var claims = jwtUtil.validateToken(auth.substring(7));
            String sub = claims.getSubject();
            Long technicianId = Long.valueOf(sub);
            var maybe = technicianRepository.findById(technicianId);
            if (maybe.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Technician not found"));
            }
            
            Technician tech = maybe.get();
            
            // Update allowed fields
            if (updatedTech.getName() != null && !updatedTech.getName().trim().isEmpty()) {
                tech.setName(updatedTech.getName());
            }
            if (updatedTech.getEmail() != null && !updatedTech.getEmail().trim().isEmpty()) {
                // Check if email is already in use by another technician
                var byEmail = technicianRepository.findByEmail(updatedTech.getEmail());
                if (byEmail.isPresent() && !byEmail.get().getId().equals(technicianId)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Email already in use"));
                }
                tech.setEmail(updatedTech.getEmail());
            }
            if (updatedTech.getPhone() != null && !updatedTech.getPhone().trim().isEmpty()) {
                // Check if phone is already in use by another technician
                var byPhone = technicianRepository.findByPhone(updatedTech.getPhone());
                if (byPhone.isPresent() && !byPhone.get().getId().equals(technicianId)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Phone already in use"));
                }
                tech.setPhone(updatedTech.getPhone());
            }
            
            Technician saved = technicianRepository.save(tech);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }

    /**
     * Change password for the current technician
     */
    @PutMapping("/api/technician/change-password")
    public ResponseEntity<?> changePassword(@RequestHeader(value = "Authorization", required = false) String auth,
                                           @RequestBody Map<String, String> body) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Authorization header required"));
        }
        
        String oldPassword = body.getOrDefault("oldPassword", "");
        String newPassword = body.getOrDefault("newPassword", "");
        
        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Old and new passwords are required"));
        }
        
        if (newPassword.length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 4 characters"));
        }
        
        try {
            var claims = jwtUtil.validateToken(auth.substring(7));
            String sub = claims.getSubject();
            Long technicianId = Long.valueOf(sub);
            var maybe = technicianRepository.findById(technicianId);
            if (maybe.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Technician not found"));
            }
            
            Technician tech = maybe.get();
            
            // Verify old password
            if (!passwordEncoder.matches(oldPassword, tech.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "Current password is incorrect"));
            }
            
            // Set new password (encoded)
            tech.setPassword(passwordEncoder.encode(newPassword));
            technicianRepository.save(tech);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }
}
