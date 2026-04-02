package com.homy.backend.controller;

import com.homy.backend.model.Customer;
import com.homy.backend.model.Booking;
import com.homy.backend.repository.CustomerRepository;
import com.homy.backend.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    // Return all customers with booking counts
    @GetMapping
    public List<CustomerSummary> listCustomers() {
        List<Customer> customers = customerRepository.findAll();
        return customers.stream().map(c -> {
            long count = bookingRepository.countByCustomerId(c.getId());
            return new CustomerSummary(c, count);
        }).collect(Collectors.toList());
    }

    // Return bookings for a specific customer
    @GetMapping("/{id}/bookings")
    public ResponseEntity<List<Booking>> getBookings(@PathVariable Long id) {
        if (!customerRepository.existsById(id)) return ResponseEntity.notFound().build();
        List<Booking> bookings = bookingRepository.findByCustomerId(id);
        return ResponseEntity.ok(bookings);
    }

    public static class CustomerSummary {
        public Customer customer;
        public long bookingCount;
        public CustomerSummary(Customer c, long count) { this.customer = c; this.bookingCount = count; }
    }
}
