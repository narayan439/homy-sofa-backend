package com.homy.backend.controller;

import com.homy.backend.model.Booking;
import com.homy.backend.model.User;
import com.homy.backend.repository.BookingRepository;
import com.homy.backend.repository.ServiceRepository;
import com.homy.backend.repository.UserRepository;
import com.homy.backend.model.ServiceEntity;
// booking sequence not required when using DB id
import com.homy.backend.service.EmailService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;

    

    @Autowired
    private EmailService emailService;

    @Autowired
    private com.homy.backend.repository.AddressRepository addressRepository;

    @Autowired
    private com.homy.backend.repository.TechnicianRepository technicianRepository;

    @Autowired
    private com.homy.backend.service.AssignmentService assignmentService;

    @GetMapping
    public List<Booking> getAll() {
        List<Booking> all = bookingRepository.findAll();
        // Fetch all addresses in a single query instead of per-booking
        java.util.Map<Long, com.homy.backend.model.Address> addressMap = new java.util.HashMap<>();
        try {
            addressRepository.findAll().forEach(a -> {
                if (a.getBookingId() != null) {
                    addressMap.put(a.getBookingId(), a);
                }
            });
        } catch (Exception e) {
            // ignore address lookup failures
        }

        // Attach address from cache for each booking
        for (Booking b : all) {
            if (b.getId() != null && addressMap.containsKey(b.getId())) {
                com.homy.backend.model.Address a = addressMap.get(b.getId());
                b.setAddress(a.getAddressText());
                b.setLatLong(a.getLatLong());
            }
            // Attach technician info if available
            try {
                if (b.getTechnicianId() != null) {
                    technicianRepository.findById(b.getTechnicianId()).ifPresent(t -> {
                        if (t.getName() != null) b.setTechnicianName(t.getName());
                        if (t.getPhone() != null) b.setTechnicianPhone(t.getPhone());
                    });
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return all;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchBooking(
            @RequestParam(value = "trackingId", required = false) String trackingId,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "reference", required = false) String reference,
            @RequestParam(value = "q", required = false) String q
    ) {
        // Require both reference/trackingId and phone for verification
        String ref = trackingId != null && !trackingId.isBlank() ? trackingId : reference;
        if (ref == null || ref.isBlank() || phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body("trackingId/reference and phone are required");
        }

        return bookingRepository.findByReferenceAndPhone(ref, phone)
                .map(b -> {
                    try {
                        var maybeAddr = addressRepository.findByBookingId(b.getId());
                        if (maybeAddr.isPresent()) {
                            var a = maybeAddr.get();
                            b.setAddress(a.getAddressText());
                            b.setLatLong(a.getLatLong());
                        }
                    } catch (Exception e) {
                        // ignore address lookup failures
                    }
                    // Attach technician info if available
                    try {
                        if (b.getTechnicianId() != null) {
                            technicianRepository.findById(b.getTechnicianId()).ifPresent(t -> {
                                if (t.getName() != null) b.setTechnicianName(t.getName());
                                if (t.getPhone() != null) b.setTechnicianPhone(t.getPhone());
                            });
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    return ResponseEntity.ok(b);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Booking> getById(@PathVariable Long id) {
        return bookingRepository.findById(id)
                .map(b -> {
                    // Attach address info if present
                    try {
                        var maybeAddr = addressRepository.findByBookingId(b.getId());
                        if (maybeAddr.isPresent()) {
                            var a = maybeAddr.get();
                            b.setAddress(a.getAddressText());
                            b.setLatLong(a.getLatLong());
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    // Attach technician info if available
                    try {
                        if (b.getTechnicianId() != null) {
                            technicianRepository.findById(b.getTechnicianId()).ifPresent(t -> {
                                if (t.getName() != null) b.setTechnicianName(t.getName());
                                if (t.getPhone() != null) b.setTechnicianPhone(t.getPhone());
                            });
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    return ResponseEntity.ok(b);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody Booking booking, HttpServletRequest request) {
        System.out.println("[BookingController.create] Starting booking creation...");
        
        if (booking.getStatus() == null) booking.setStatus("PENDING");

        // Extract userId from JWT token if available
        Long userId = null;
        Claims claims = (Claims) request.getAttribute("claims");
        System.out.println("[BookingController.create] Claims from request: " + claims);
        
        if (claims != null) {
            String userIdStr = claims.getSubject();
            System.out.println("[BookingController.create] Subject from claims: " + userIdStr);
            
            if (userIdStr != null && !userIdStr.isBlank()) {
                try {
                    userId = Long.parseLong(userIdStr);
                    System.out.println("[BookingController.create] Parsed userId: " + userId);
                } catch (NumberFormatException e) {
                    System.err.println("[BookingController.create] Could not parse userId from JWT: " + userIdStr);
                }
            }
        } else {
            System.out.println("[BookingController.create] No claims found in request (user not authenticated)");
        }

        // Link booking to authenticated user
        if (userId != null) {
            System.out.println("[BookingController.create] Looking up user with ID: " + userId);
            var userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("[BookingController.create] User found: " + user.getEmail() + " (ID: " + user.getId() + ")");
                booking.setUser(user);
                booking.setUserId(userId);  // Explicitly set userId for proper persistence
                System.out.println("[BookingController.create] Set booking userId: " + booking.getUserId());
            } else {
                System.err.println("[BookingController.create] User not found for ID: " + userId);
                userId = null;  // Reset userId if user not found
            }
        }

        // If booking.service looks like an internal id, try to resolve service entity
        if (booking.getService() != null) {
            try {
                ServiceEntity svc = serviceRepository.findById(booking.getService()).orElse(null);
                if (svc != null) {
                    booking.setService(svc.getName());
                }
            } catch (Exception e) {
                // swallow - if lookup fails we'll keep the original value
            }
        }

        // Save booking
        Booking saved = bookingRepository.save(booking);
        System.out.println("[BookingController.create] Booking saved with ID: " + saved.getId() + ", userId: " + saved.getUserId());

        // Generate reference using DB-generated id
        int year = LocalDateTime.now().getYear();
        long idVal = saved.getId() != null ? saved.getId() : 0L;
        String seq = String.valueOf(idVal);
        String reference = "HOMY" + year + seq;
        saved.setReference(reference);
        bookingRepository.save(saved);
        System.out.println("[BookingController.create] Final booking - ID: " + saved.getId() + ", userId: " + saved.getUserId() + ", reference: " + reference);

        // Send confirmation email asynchronously
        try { emailService.sendBookingConfirmation(saved); } catch (Exception e) {}

        // Persist address if provided
        try {
            if ((booking.getAddress() != null && !booking.getAddress().isBlank()) ||
                (booking.getLatLong() != null && !booking.getLatLong().isBlank())) {

                com.homy.backend.model.Address addr = new com.homy.backend.model.Address();
                addr.setBookingId(saved.getId());
                addr.setAddressText(booking.getAddress());
                addr.setLatLong(booking.getLatLong());
                addressRepository.save(addr);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Attempt automatic technician assignment
        try {
            assignmentService.assignTechnicianForBooking(saved);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> update(
            @PathVariable Long id,
            @RequestBody Booking booking,
            @RequestParam(name = "sendEmail", required = false) Boolean sendEmail
    ) {
        return bookingRepository.findById(id).map(existing -> {
            String oldStatus = existing.getStatus();
            String newStatus = booking.getStatus();

            // Only update fields provided in the request (avoid overwriting with nulls)
            if (booking.getStatus() != null) existing.setStatus(booking.getStatus());
            if (booking.getMessage() != null) existing.setMessage(booking.getMessage());
            if (booking.getDate() != null) existing.setDate(booking.getDate());
            if (booking.getService() != null) existing.setService(booking.getService());
            if (booking.getName() != null) existing.setName(booking.getName());
            if (booking.getPhone() != null) existing.setPhone(booking.getPhone());
            if (booking.getEmail() != null) existing.setEmail(booking.getEmail());
            if (booking.getTotalAmount() != null) {
                // When admin provides a completed/total amount, store it in both totalAmount and price fields
                existing.setTotalAmount(booking.getTotalAmount());
                existing.setPrice(booking.getTotalAmount());
            }

            // Store admin approval/status update fields
            if (booking.getInstruments() != null) existing.setInstruments(booking.getInstruments());
            if (booking.getExtraAmount() != null) existing.setExtraAmount(booking.getExtraAmount());
            if (booking.getAdditionalService() != null) existing.setAdditionalService(booking.getAdditionalService());
            if (booking.getCancelReason() != null) existing.setCancelReason(booking.getCancelReason());
            if (booking.getAdminNotes() != null) existing.setAdminNotes(booking.getAdminNotes());
            if (booking.getAdditionalServiceName() != null) existing.setAdditionalServiceName(booking.getAdditionalServiceName());
            if (booking.getAdditionalServicePrice() != null) existing.setAdditionalServicePrice(booking.getAdditionalServicePrice());
            if (booking.getAdditionalServicesJson() != null) existing.setAdditionalServicesJson(booking.getAdditionalServicesJson());
            if (booking.getCompletionDate() != null) existing.setCompletionDate(booking.getCompletionDate());
            if (booking.getTechnicianId() != null) existing.setTechnicianId(booking.getTechnicianId());
            if (booking.getTechnicianStatus() != null) existing.setTechnicianStatus(booking.getTechnicianStatus());

            // If admin changed booking.status to COMPLETED or CANCELLED, ensure technicianStatus follows
            if (newStatus != null) {
                String ns = newStatus.toUpperCase();
                if (ns.equals("COMPLETED")) {
                    existing.setTechnicianStatus("COMPLETED");
                    if (existing.getCompletionDate() == null) {
                        existing.setCompletionDate(LocalDateTime.now().toString());
                    }
                } else if (ns.equals("CANCELLED")) {
                    existing.setTechnicianStatus("CANCELLED");
                }
            }

            Booking updated = bookingRepository.save(existing);

            // Decide whether to send email: if sendEmail param provided, use it; otherwise
            // only send emails for APPROVED/COMPLETED/CANCELLED transitions
            boolean shouldSend = false;
            if (oldStatus != null && newStatus != null && !oldStatus.equals(newStatus)) {
                if (sendEmail != null) {
                    shouldSend = sendEmail.booleanValue();
                } else {
                    String next = newStatus.toUpperCase();
                    // Send emails for ASSIGNED (previously APPROVED), COMPLETED or CANCELLED
                    shouldSend = next.equals("APPROVED") || next.equals("ASSIGNED") || next.equals("COMPLETED") || next.equals("CANCELLED");
                }
            }

            if (shouldSend) {
                try {
                    emailService.sendStatusChangeEmail(updated, oldStatus, newStatus);
                } catch (Exception e) {
                    // Log but don't fail the update if email fails
                    e.printStackTrace();
                }
            }

            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return bookingRepository.findById(id).map(b -> {
            bookingRepository.delete(b);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // User Bookings -User dashboard to view their bookings
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserBookings(@PathVariable Long userId) {
        try {
            List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
            // Enrich bookings with technician info
            for (Booking b : bookings) {
                try {
                    if (b.getTechnicianId() != null) {
                        technicianRepository.findById(b.getTechnicianId()).ifPresent(t -> {
                            if (t.getName() != null) b.setTechnicianName(t.getName());
                            if (t.getPhone() != null) b.setTechnicianPhone(t.getPhone());
                        });
                    }
                } catch (Exception e) {
                    // ignore technician lookup failures
                }
            }
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching user bookings: " + e.getMessage());
        }
    }
}
