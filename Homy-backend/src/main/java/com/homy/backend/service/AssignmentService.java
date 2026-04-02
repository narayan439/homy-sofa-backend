package com.homy.backend.service;

import com.homy.backend.model.Booking;
import com.homy.backend.model.Technician;
import com.homy.backend.repository.BookingRepository;
import com.homy.backend.repository.TechnicianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Try to assign the best technician for the given booking.
     * Strategy: filter active technicians by service category, choose the one
     * with the least number of active jobs (ASSIGNED/ACCEPTED/IN_PROGRESS).
     */
    @Transactional
    public Optional<Technician> assignTechnicianForBooking(Booking booking) {
        if (booking == null) return Optional.empty();
        String category = booking.getService();
        List<Technician> candidates = null;
        if (category != null && !category.isBlank()) {
            // try exact category match first (repository method expects exact string)
            candidates = technicianRepository.findByServiceCategoryAndIsActiveTrue(category.trim());
        }
        // If no candidates found for the category, fallback to any active technicians
        if (candidates == null || candidates.isEmpty()) {
            List<Technician> all = technicianRepository.findAll();
            candidates = all.stream().filter(t -> t.getIsActive() != null && t.getIsActive()).toList();
        }
        if (candidates == null || candidates.isEmpty()) return Optional.empty();

        // Compute load for each candidate
        Technician best = candidates.stream()
                .min(Comparator.comparingInt(t -> activeJobsCount(t.getId())))
                .orElse(null);

        if (best == null) return Optional.empty();

        // Assign and persist
        booking.setTechnicianId(best.getId());
        booking.setTechnicianStatus("ASSIGNED");
        bookingRepository.save(booking);

        // Notify technician via email if available
        try {
            if (best.getEmail() != null && !best.getEmail().isBlank()) {
                emailService.sendTechnicianAssignment(best, booking);
            }
        } catch (Exception e) {
            // don't fail assignment on notification errors
        }

        return Optional.of(best);
    }

    private int activeJobsCount(Long technicianId) {
        if (technicianId == null) return Integer.MAX_VALUE;
        List<Booking> list = bookingRepository.findByTechnicianId(technicianId);
        int count = 0;
        for (Booking b : list) {
            String s = b.getTechnicianStatus();
            if (s == null) s = b.getStatus();
            if (s != null) {
                String up = s.toUpperCase();
                if (up.equals("ASSIGNED") || up.equals("ACCEPTED") || up.equals("IN_PROGRESS")) count++;
            }
        }
        return count;
    }
}
