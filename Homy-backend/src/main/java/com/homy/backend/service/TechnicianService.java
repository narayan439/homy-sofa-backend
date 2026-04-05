package com.homy.backend.service;

import com.homy.backend.model.Booking;
import com.homy.backend.model.Technician;
import com.homy.backend.repository.BookingRepository;
import com.homy.backend.repository.TechnicianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class TechnicianService {

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private com.homy.backend.repository.CustomerRepository customerRepository;

    @Autowired
    private com.homy.backend.repository.AddressRepository addressRepository;

    public Technician createTechnician(Technician t) {
        // generate a unique serial technician code like TECH001 if not provided
        if (t.getTechnicianId() == null || t.getTechnicianId().isEmpty()) {
            long base = technicianRepository.count();
            String code = String.format("TECH%03d", base + 1);
            int attempts = 0;
            while (technicianRepository.findByTechnicianId(code).isPresent() && attempts < 10000) {
                attempts++;
                code = String.format("TECH%03d", base + 1 + attempts);
            }
            t.setTechnicianId(code);
        }
        Technician saved = technicianRepository.save(t);
        // Invalidate cache when creating new technician
        clearAllTechniciansCache();
        return saved;
    }

    public boolean deleteTechnician(Long id) {
        if (!technicianRepository.existsById(id)) return false;
        technicianRepository.deleteById(id);
        // Invalidate cache when deleting
        clearAllTechniciansCache();
        return true;
    }

    @CacheEvict(value = "technicians", allEntries = true)
    private void clearAllTechniciansCache() {
        // This method is called implicitly by @CacheEvict annotation
    }

    public boolean setTechnicianPassword(Long id, String encodedPassword) {
        return technicianRepository.findById(id).map(t -> {
            t.setPassword(encodedPassword);
            technicianRepository.save(t);
            clearAllTechniciansCache();
            return true;
        }).orElse(false);
    }

    @CacheEvict(value = "technicians", allEntries = true)
    public Technician updateTechnician(Long id, Technician updated) {
        return technicianRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setEmail(updated.getEmail());
            existing.setPhone(updated.getPhone());
            if (updated.getPassword() != null && !updated.getPassword().isEmpty()) existing.setPassword(updated.getPassword());
            existing.setServiceCategory(updated.getServiceCategory());
            existing.setIsActive(updated.getIsActive());
            return technicianRepository.save(existing);
        }).orElse(null);
    }

    @Cacheable(value = "technicians", unless = "#result == null || #result.isEmpty()")
    public List<Technician> getAllTechnicians() {
        return technicianRepository.findAll();
    }

    public boolean assignTechnicianToBooking(Long bookingId, Long technicianId) {
        Optional<Booking> ob = bookingRepository.findById(bookingId);
        if (ob.isEmpty()) return false;
        Booking b = ob.get();
        b.setTechnicianId(technicianId);
        b.setTechnicianStatus("ASSIGNED");
        bookingRepository.save(b);
        return true;
    }

    public boolean acceptJob(Long bookingId, Long technicianId) {
        Optional<Booking> ob = bookingRepository.findById(bookingId);
        if (ob.isEmpty()) return false;
        Booking b = ob.get();
        if (technicianId != null && b.getTechnicianId() != null && !b.getTechnicianId().equals(technicianId)) return false;
        if (!"ASSIGNED".equals(b.getTechnicianStatus())) return false;
        b.setTechnicianStatus("ACCEPTED");
        bookingRepository.save(b);
        return true;
    }

    public boolean addAdditionalService(Long bookingId, Long technicianId, String serviceName, Double price) {
        Optional<Booking> ob = bookingRepository.findById(bookingId);
        if (ob.isEmpty()) return false;
        Booking b = ob.get();
        if (technicianId != null && b.getTechnicianId() != null && !b.getTechnicianId().equals(technicianId)) return false;
        // Prevent adding an extra that is the same as the main booked service
        String mainService = b.getService();
        if (mainService != null && serviceName != null && mainService.trim().equalsIgnoreCase(serviceName.trim())) {
            throw new IllegalArgumentException("duplicate-main-service");
        }

        // Parse existing services JSON or create new
        String currentJson = b.getAdditionalServicesJson();
        java.util.List<java.util.Map<String, Object>> services = new java.util.ArrayList<>();
        
        if (currentJson != null && !currentJson.trim().isEmpty() && !currentJson.equals("[]")) {
            try {
                services = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    currentJson, java.util.List.class
                );
            } catch (Exception e) {
                // unable to parse; start fresh
                services = new java.util.ArrayList<>();
            }
        }
        
        // Check duplicate among existing extras
        if (services != null) {
            for (java.util.Map<String, Object> s : services) {
                Object nm = s.get("name");
                if (nm != null && serviceName != null && String.valueOf(nm).trim().equalsIgnoreCase(serviceName.trim())) {
                    throw new IllegalArgumentException("duplicate-extra-service");
                }
            }
        }

        // Add new service
        java.util.Map<String, Object> newService = new java.util.HashMap<>();
        newService.put("id", String.valueOf(services.size() + 1));
        newService.put("name", serviceName);
        newService.put("price", price);
        services.add(newService);
        
        // Serialize back to JSON
        try {
            String updatedJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(services);
            b.setAdditionalServicesJson(updatedJson);
        } catch (Exception e) {
            return false;
        }
        
        bookingRepository.save(b);
        return true;
    }

    public List<Booking> getBookingsForTechnician(Long technicianId) {
        // Backwards-compatible method: return full list (not paginated)
        List<Booking> list = bookingRepository.findByTechnicianId(technicianId);

        // Extract all booking IDs and customer IDs for batch queries (avoid N+1)
        java.util.Set<Long> bookingIds = new java.util.HashSet<>();
        java.util.Set<Long> customerIds = new java.util.HashSet<>();
        for (Booking b : list) {
            bookingIds.add(b.getId());
            if (b.getCustomerId() != null) customerIds.add(b.getCustomerId());
        }

        // Batch load all addresses for these bookings (single query)
        java.util.Map<Long, com.homy.backend.model.Address> addresses = new java.util.HashMap<>();
        if (!bookingIds.isEmpty()) {
            addressRepository.findByBookingIds(new java.util.ArrayList<>(bookingIds)).forEach(a -> {
                if (a.getBookingId() != null) addresses.put(a.getBookingId(), a);
            });
        }

        // Batch load all customers for these bookings (single query)
        java.util.Map<Long, com.homy.backend.model.Customer> customers = new java.util.HashMap<>();
        if (!customerIds.isEmpty()) {
            customerRepository.findAllById(customerIds).forEach(c -> customers.put(c.getId(), c));
        }

        // Enrich bookings with pre-loaded customer and address data
        for (Booking b : list) {
            // Add customer info from batch load
            if (b.getCustomerId() != null && customers.containsKey(b.getCustomerId())) {
                com.homy.backend.model.Customer c = customers.get(b.getCustomerId());
                if (c.getName() != null) b.setName(c.getName());
                if (c.getPhone() != null) b.setPhone(c.getPhone());
                if (c.getEmail() != null) b.setEmail(c.getEmail());
            }

            // Add address info from batch load
            if (addresses.containsKey(b.getId())) {
                com.homy.backend.model.Address addr = addresses.get(b.getId());
                b.setAddress(addr.getAddressText());
                b.setLatLong(addr.getLatLong());
            }
        }
        return list;
    }

    public Map<String, Object> getBookingsForTechnician(Long technicianId, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> pg = bookingRepository.findByTechnicianId(technicianId, pr);
        List<Booking> list = pg.getContent();

        // Extract all booking IDs and customer IDs for batch queries (avoid N+1)
        java.util.Set<Long> bookingIds = new java.util.HashSet<>();
        java.util.Set<Long> customerIds = new java.util.HashSet<>();
        for (Booking b : list) {
            bookingIds.add(b.getId());
            if (b.getCustomerId() != null) customerIds.add(b.getCustomerId());
        }

        // Batch load all addresses for these bookings (single query)
        java.util.Map<Long, com.homy.backend.model.Address> addresses = new java.util.HashMap<>();
        if (!bookingIds.isEmpty()) {
            addressRepository.findByBookingIds(new java.util.ArrayList<>(bookingIds)).forEach(a -> {
                if (a.getBookingId() != null) addresses.put(a.getBookingId(), a);
            });
        }

        // Batch load all customers for these bookings (single query)
        java.util.Map<Long, com.homy.backend.model.Customer> customers = new java.util.HashMap<>();
        if (!customerIds.isEmpty()) {
            customerRepository.findAllById(customerIds).forEach(c -> customers.put(c.getId(), c));
        }

        // Enrich bookings with pre-loaded customer and address data
        for (Booking b : list) {
            // Add customer info from batch load
            if (b.getCustomerId() != null && customers.containsKey(b.getCustomerId())) {
                com.homy.backend.model.Customer c = customers.get(b.getCustomerId());
                if (c.getName() != null) b.setName(c.getName());
                if (c.getPhone() != null) b.setPhone(c.getPhone());
                if (c.getEmail() != null) b.setEmail(c.getEmail());
            }

            // Add address info from batch load (booking-specific address)
            if (addresses.containsKey(b.getId())) {
                com.homy.backend.model.Address addr = addresses.get(b.getId());
                b.setAddress(addr.getAddressText());
                b.setLatLong(addr.getLatLong());
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("total", pg.getTotalElements());
        res.put("page", pg.getNumber());
        res.put("size", pg.getSize());
        res.put("bookings", list);
        return res;
    }

    public boolean startJob(Long bookingId, Long technicianId) {
        Optional<Booking> ob = bookingRepository.findById(bookingId);
        if (ob.isEmpty()) return false;
        Booking b = ob.get();
        if (technicianId != null && b.getTechnicianId() != null && !b.getTechnicianId().equals(technicianId)) return false;
        b.setTechnicianStatus("IN_PROGRESS");
        bookingRepository.save(b);
        return true;
    }

    public boolean completeJob(Long bookingId, Long technicianId, java.util.Map<String, Object> payload) {
        Optional<Booking> ob = bookingRepository.findById(bookingId);
        if (ob.isEmpty()) return false;
        Booking b = ob.get();
        if (technicianId != null && b.getTechnicianId() != null && !b.getTechnicianId().equals(technicianId)) return false;

        // Update provided completion details
        if (payload != null) {
            if (payload.containsKey("totalAmount")) {
                try { b.setTotalAmount(Double.valueOf(String.valueOf(payload.get("totalAmount")))); b.setPrice(b.getTotalAmount()); } catch (Exception e) {}
            }
            if (payload.containsKey("technicianNotes")) {
                String notes = String.valueOf(payload.get("technicianNotes"));
                b.setTechnicianNotes(notes);
            }
            if (payload.containsKey("additionalServiceName")) {
                b.setAdditionalServiceName(String.valueOf(payload.get("additionalServiceName")));
                b.setAdditionalService(true);
            }
            if (payload.containsKey("additionalServicePrice")) {
                try { b.setAdditionalServicePrice(Double.valueOf(String.valueOf(payload.get("additionalServicePrice")))); } catch (Exception e) {}
            }
            if (payload.containsKey("additionalServicesJson")) {
                b.setAdditionalServicesJson(String.valueOf(payload.get("additionalServicesJson")));
            }
            
            // ===== PAYMENT FIELDS =====
            if (payload.containsKey("paymentMethod")) {
                b.setPaymentMethod(String.valueOf(payload.get("paymentMethod"))); // CASH or ONLINE
            }
            if (payload.containsKey("paymentId")) {
                b.setPaymentId(String.valueOf(payload.get("paymentId"))); // Razorpay payment ID
            }
            if (payload.containsKey("transactionId")) {
                b.setTransactionId(String.valueOf(payload.get("transactionId"))); // Razorpay order ID
            }
            if (payload.containsKey("paymentStatus")) {
                b.setPaymentStatus(String.valueOf(payload.get("paymentStatus"))); // SUCCESS, FAILED, etc
            }
            // Set payment timestamp to now when payment is processed
            if (payload.containsKey("paymentMethod")) {
                b.setPaymentTimestamp(java.time.LocalDateTime.now());
            }
        }

        b.setTechnicianStatus("COMPLETED");
        b.setStatus("COMPLETED");
        java.time.LocalDate today = java.time.LocalDate.now();
        b.setCompletionDate(today.toString());
        bookingRepository.save(b);
        return true;
    }

    public boolean cancelJob(Long bookingId, Long technicianId, String reason) {
        Optional<Booking> ob = bookingRepository.findById(bookingId);
        if (ob.isEmpty()) return false;
        Booking b = ob.get();
        if (technicianId != null && b.getTechnicianId() != null && !b.getTechnicianId().equals(technicianId)) return false;
        b.setTechnicianStatus("CANCELLED");
        b.setStatus("CANCELLED");
        if (reason != null) b.setCancelReason(reason);
        bookingRepository.save(b);
        return true;
    }
}
