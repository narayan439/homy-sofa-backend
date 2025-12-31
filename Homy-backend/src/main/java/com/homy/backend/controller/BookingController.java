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

    @Autowired
    private com.homy.backend.repository.AddressRepository addressRepository;

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
            // Attach address info if present
            try {
                if (b.getId() != null) {
                    addressRepository.findByBookingId(b.getId()).ifPresent(a -> {
                        b.setAddress(a.getAddressText());
                        b.setLatLong(a.getLatLong());
                    });
                }
            } catch (Exception e) {
                // ignore address lookup failures
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
                    // Attach address info if present
                    try {
                        addressRepository.findByBookingId(b.getId()).ifPresent(a -> {
                            b.setAddress(a.getAddressText());
                            b.setLatLong(a.getLatLong());
                        });
                    } catch (Exception e) {
                        // ignore
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

        // If booking.service looks like an internal id, try to resolve service entity
        if (booking.getService() != null) {
            try {
                ServiceEntity svc = serviceRepository.findById(booking.getService()).orElse(null);
                if (svc != null) {
                    // Always store the human-readable service name for persistence and emails
                    booking.setService(svc.getName());
                    // If price wasn't provided, set it from the service
                    if (booking.getPrice() == null && svc.getPrice() != null) {
                        booking.setPrice(svc.getPrice());
                    }
                }
            } catch (Exception e) {
                // swallow - if lookup fails we'll keep the original value
            }
        }

        // Prevent customer from booking the same service multiple times while they have an active booking
        Booking existingBooking = bookingRepository.findActiveBookingForService(customer.getId(), booking.getService());
        if (existingBooking != null) {
            // Return 409 with the existing booking reference
            return ResponseEntity.status(409).body(existingBooking);
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

        // Persist address if provided (store lat,long as comma separated and full address text)
        try {
            if ((booking.getAddress() != null && !booking.getAddress().isBlank()) ||
                (booking.getLatLong() != null && !booking.getLatLong().isBlank())) {

                com.homy.backend.model.Address addr = new com.homy.backend.model.Address();
                addr.setBookingId(saved.getId());
                addr.setCustomerId(saved.getCustomerId());
                addr.setAddressText(booking.getAddress());
                addr.setLatLong(booking.getLatLong());
                addressRepository.save(addr);
            }
        } catch (Exception ex) {
            // Do not fail booking creation if address persistence fails; log and continue
            ex.printStackTrace();
        }

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> update(@PathVariable Long id, @RequestBody Booking booking) {
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
            if (booking.getTotalAmount() != null) existing.setTotalAmount(booking.getTotalAmount());
            
            Booking updated = bookingRepository.save(existing);
            
            // Send status change email if status has changed
            if (oldStatus != null && newStatus != null && !oldStatus.equals(newStatus)) {
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
}
