package com.homy.backend.service;

import com.homy.backend.model.Booking;
import com.homy.backend.model.Technician;
import com.homy.backend.repository.BookingRepository;
import com.homy.backend.repository.TechnicianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AssignmentServiceTest {

    private TechnicianRepository techRepo;
    private BookingRepository bookingRepo;
    private EmailService emailService;
    private AssignmentService service;

    @BeforeEach
    public void setup() {
        techRepo = mock(TechnicianRepository.class);
        bookingRepo = mock(BookingRepository.class);
        emailService = mock(EmailService.class);
        service = new AssignmentService();
        // inject via reflection
        try {
            java.lang.reflect.Field f1 = AssignmentService.class.getDeclaredField("technicianRepository");
            f1.setAccessible(true); f1.set(service, techRepo);
            java.lang.reflect.Field f2 = AssignmentService.class.getDeclaredField("bookingRepository");
            f2.setAccessible(true); f2.set(service, bookingRepo);
            java.lang.reflect.Field f3 = AssignmentService.class.getDeclaredField("emailService");
            f3.setAccessible(true); f3.set(service, emailService);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    public void assignsLeastLoadedTechnician() {
        Booking b = new Booking(); b.setId(10L); b.setService("plumbing");
        Technician t1 = new Technician(); t1.setId(1L); t1.setName("A"); t1.setEmail("a@x.com");
        Technician t2 = new Technician(); t2.setId(2L); t2.setName("B"); t2.setEmail("b@x.com");

        when(techRepo.findByServiceCategoryAndIsActiveTrue("plumbing")).thenReturn(Arrays.asList(t1, t2));
        when(bookingRepo.findByTechnicianId(1L)).thenReturn(Arrays.asList());
        when(bookingRepo.findByTechnicianId(2L)).thenReturn(Arrays.asList(new Booking()));
        when(bookingRepo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Optional<Technician> assigned = service.assignTechnicianForBooking(b);
        assertTrue(assigned.isPresent());
        assertEquals(1L, assigned.get().getId());

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepo).save(captor.capture());
        Booking saved = captor.getValue();
        assertEquals(1L, saved.getTechnicianId());
        assertEquals("ASSIGNED", saved.getTechnicianStatus());
        verify(emailService).sendTechnicianAssignment(any(), any());
    }
}
