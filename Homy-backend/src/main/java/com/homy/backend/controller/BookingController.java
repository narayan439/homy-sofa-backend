package com.homy.backend.controller;

import com.homy.backend.model.Booking;
import com.homy.backend.model.Customer;
import com.homy.backend.repository.BookingRepository;
import com.homy.backend.repository.CustomerRepository;
import com.homy.backend.repository.ServiceRepository;
import com.homy.backend.model.ServiceEntity;
// booking sequence not required when using DB id
import com.homy.backend.service.EmailService;
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
    private CustomerRepository customerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    

    @Autowired
    private EmailService emailService;

    @GetMapping
    public List<Booking> getAll() {
        List<Booking> all = bookingRepository.findAll();
        // Enrich bookings with customer details when available
        for (Booking b : all) {
            if (b.getCustomerId() != null) {
                customerRepository.findById(b.getCustomerId()).ifPresent(c -> {
                    if (c.getName() != null) b.setName(c.getName());
                    if (c.getPhone() != null) b.setPhone(c.getPhone());
                    if (c.getEmail() != null) b.setEmail(c.getEmail());
                });
            }
        }
        return all;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getById(@PathVariable Long id) {
        return bookingRepository.findById(id)
                .map(b -> {
                    if (b.getCustomerId() != null) {
                        customerRepository.findById(b.getCustomerId()).ifPresent(c -> {
                            if (c.getName() != null) b.setName(c.getName());
                            if (c.getPhone() != null) b.setPhone(c.getPhone());
                            if (c.getEmail() != null) b.setEmail(c.getEmail());
                        });
                    }
                    return ResponseEntity.ok(b);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Booking> create(@RequestBody Booking booking) {
        if (booking.getStatus() == null) booking.setStatus("PENDING");

        // Find or create customer by phone (unique identifier)
        String phone = booking.getPhone();
        Customer customer = customerRepository.findByPhone(phone)
            .orElseGet(() -> customerRepository.save(new Customer(booking.getName(), phone, booking.getEmail())));

        // Link booking to customer
        booking.setCustomerId(customer.getId());

        // Set price from service if not already set
        if (booking.getPrice() == null && booking.getService() != null) {
            ServiceEntity svc = serviceRepository.findById(booking.getService()).orElse(null);
            if (svc != null && svc.getPrice() != null) {
                booking.setPrice(svc.getPrice());
            }
        }

        // Prevent customer from booking the same service multiple times while they have an active booking
        if (bookingRepository.hasActiveBookingForService(customer.getId(), booking.getService())) {
            return ResponseEntity.status(409).build(); // Conflict: duplicate active booking for same service
        }

        // Save booking first (so DB persists any fields), then obtain DB id for reference
        Booking saved = bookingRepository.save(booking);

        // Generate reference using DB-generated id and save again.
        // Format: HOMY{YEAR}{seq padded to 6 digits} e.g. HOMY202500001
        int year = LocalDateTime.now().getYear();
        long idVal = saved.getId() != null ? saved.getId() : 0L;
        String seqPadded = String.format("%06d", idVal);
        String reference = "HOMY" + year + seqPadded;
        saved.setReference(reference);
        bookingRepository.save(saved);

        // Send confirmation email asynchronously (simple fire-and-forget)
        try { emailService.sendBookingConfirmation(saved); } catch (Exception e) {}
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> update(@PathVariable Long id, @RequestBody Booking booking) {
        return bookingRepository.findById(id).map(existing -> {
            // Only update fields provided in the request (avoid overwriting with nulls)
            if (booking.getStatus() != null) existing.setStatus(booking.getStatus());
            if (booking.getMessage() != null) existing.setMessage(booking.getMessage());
            if (booking.getDate() != null) existing.setDate(booking.getDate());
            if (booking.getService() != null) existing.setService(booking.getService());
            if (booking.getName() != null) existing.setName(booking.getName());
            if (booking.getPhone() != null) existing.setPhone(booking.getPhone());
            if (booking.getEmail() != null) existing.setEmail(booking.getEmail());
            if (booking.getTotalAmount() != null) existing.setTotalAmount(booking.getTotalAmount());
            bookingRepository.save(existing);
            return ResponseEntity.ok(existing);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return bookingRepository.findById(id).map(b -> {
            bookingRepository.delete(b);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
