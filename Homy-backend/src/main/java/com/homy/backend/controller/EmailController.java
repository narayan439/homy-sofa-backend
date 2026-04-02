package com.homy.backend.controller;

import com.homy.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class EmailController {

    @Autowired
    private EmailService emailService;

    /**
     * Simple test endpoint to send a test booking confirmation email.
     * POST /api/test-email
     * Body: { "email": "user@example.com", "name": "User", "service": "Cleaning" }
     */
    @PostMapping("/test-email")
    public ResponseEntity<?> sendTestEmail(@RequestBody Map<String, Object> body) {
        // Defensive parsing: accept non-string JSON values and convert to string
        try {
            System.out.println("/api/test-email body=" + body);
            Object emailObj = body.get("email");
            if (emailObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "email is required"));
            }
            String email = String.valueOf(emailObj);
            String name = body.containsKey("name") ? String.valueOf(body.get("name")) : "Customer";
            String service = body.containsKey("service") ? String.valueOf(body.get("service")) : "Service";

            // Create a lightweight booking-like object for the template
            com.homy.backend.model.Booking b = new com.homy.backend.model.Booking();
            b.setEmail(email);
            b.setName(name);
            b.setService(service);
            b.setReference("TEST-REF");
            b.setDate(java.time.LocalDate.now().toString());

            emailService.sendBookingConfirmation(b);
            return ResponseEntity.ok(Map.of("status", "sent"));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }
}
