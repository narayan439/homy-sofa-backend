package com.homy.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;
    private String email;
    private String service;

    @Column(length = 256)
    private String message;

    private String date; // keep as string to match frontend format


    private String status; // PENDING / APPROVED / COMPLETED / CANCELLED

    @Column(name = "price", columnDefinition = "DOUBLE")
    private Double price;

    @Column(name = "total_amount", columnDefinition = "DOUBLE")
    private Double totalAmount;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "reference", unique = true)
    private String reference; // e.g. HOMY202500001

    @Column(name = "created_at", columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Booking() {}

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    // Transient fields to receive/store address information from client
    @Transient
    private String address;

    @Transient
    private String latLong; // format: "lat,lon"

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getLatLong() { return latLong; }
    public void setLatLong(String latLong) { this.latLong = latLong; }
}
