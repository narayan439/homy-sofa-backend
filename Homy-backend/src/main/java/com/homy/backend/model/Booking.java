package com.homy.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    
    @Column(name = "time_slot")
    private String timeSlot; // morning/afternoon/evening or custom


    private String status; // PENDING / APPROVED / COMPLETED / CANCELLED

    @Column(name = "price", columnDefinition = "DOUBLE")
    private Double price;

    @Column(name = "total_amount", columnDefinition = "DOUBLE")
    private Double totalAmount;


    @Column(name = "user_id")
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "reference", unique = true)
    private String reference; // e.g. HOMY202500001

    @Column(name = "created_at", columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Provide a serialized bookingDate field (transient) derived from createdAt for frontend compatibility
    @Transient
    private String bookingDate;

    @Column(name = "completion_date")
    private String completionDate; // When the booking was completed (format: yyyy-MM-dd or similar)

    // Admin approval/status update fields
    @Column(length = 500)
    private String instruments; // e.g., "drill, wrench, screwdriver"

    @Column(name = "extra_amount", columnDefinition = "DOUBLE")
    private Double extraAmount; // Amount needed in addition to service price

    @Column(name = "additional_service")
    private Boolean additionalService; // Whether admin approved additional service

    @Column(length = 256)
    private String cancelReason; // Reason for cancellation

  

    @Column(length = 256)
    private String additionalServiceName; // Name of additional service approved by admin

    @Column(name = "additional_service_price", columnDefinition = "DOUBLE")
    private Double additionalServicePrice; // Price of additional service

    @Column(name = "additional_services_json", columnDefinition = "LONGTEXT")
    private String additionalServicesJson; // JSON array of multiple services: [{"id":"1","name":"Service","price":100}]

    @Column(name = "address_id")
    private Long addressId; // Reference to UserAddress selected during booking

    @Column(name = "latitude", columnDefinition = "DOUBLE")
    private Double latitude; // Latitude of delivery address

    @Column(name = "longitude", columnDefinition = "DOUBLE")
    private Double longitude; // Longitude of delivery address

    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "technician_status")
    private String technicianStatus; // ASSIGNED / IN_PROGRESS / COMPLETED

    @Column(name = "technician_notes", length = 1000)
    private String technicianNotes; // Notes provided by technician upon completion

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes; // Notes from admin

    // ===== PAYMENT FIELDS =====
    @Column(name = "payment_method", length = 20)
    private String paymentMethod; // CASH or ONLINE

    @Column(name = "payment_id", length = 256)
    private String paymentId; // Razorpay payment ID (e.g., pay_xxx) or custom ID for cash

    @Column(name = "transaction_id", length = 256)
    private String transactionId; // Razorpay transaction/order ID (e.g., order_xxx)

    @Column(name = "payment_status", length = 50)
    private String paymentStatus; // PENDING / SUCCESS / FAILED / NOT_REQUIRED (for CASH)

    @Column(name = "payment_timestamp")
    private LocalDateTime paymentTimestamp; // When payment was processed

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
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getBookingDate() {
        if (this.createdAt != null) return this.createdAt.toString();
        return this.bookingDate;
    }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }
    public String getCompletionDate() { return completionDate; }
    public void setCompletionDate(String completionDate) { this.completionDate = completionDate; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getInstruments() { return instruments; }
    public void setInstruments(String instruments) { this.instruments = instruments; }

    public Double getExtraAmount() { return extraAmount; }
    public void setExtraAmount(Double extraAmount) { this.extraAmount = extraAmount; }

    public Boolean getAdditionalService() { return additionalService; }
    public void setAdditionalService(Boolean additionalService) { this.additionalService = additionalService; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }



    public String getAdditionalServiceName() { return additionalServiceName; }
    public void setAdditionalServiceName(String additionalServiceName) { this.additionalServiceName = additionalServiceName; }

    public Double getAdditionalServicePrice() { return additionalServicePrice; }
    public void setAdditionalServicePrice(Double additionalServicePrice) { this.additionalServicePrice = additionalServicePrice; }

    public String getAdditionalServicesJson() { return additionalServicesJson; }
    public void setAdditionalServicesJson(String additionalServicesJson) { this.additionalServicesJson = additionalServicesJson; }

    public Long getTechnicianId() { return technicianId; }
    public void setTechnicianId(Long technicianId) { this.technicianId = technicianId; }

    public String getTechnicianStatus() { return technicianStatus; }
    public void setTechnicianStatus(String technicianStatus) { this.technicianStatus = technicianStatus; }

    public String getTechnicianNotes() { return technicianNotes; }
    public void setTechnicianNotes(String technicianNotes) { this.technicianNotes = technicianNotes; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    // ===== PAYMENT GETTERS/SETTERS =====
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getPaymentTimestamp() { return paymentTimestamp; }
    public void setPaymentTimestamp(LocalDateTime paymentTimestamp) { this.paymentTimestamp = paymentTimestamp; }

    // Transient fields to receive/store address information from client
    @Transient
    private String address;

    @Transient
    private String latLong; // format: "lat,lon"

    @Transient
    private String technicianName;

    @Transient
    private String technicianPhone;

    @Transient
    private String assignedAt;

    @Transient
    private String startedAt;

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getLatLong() { return latLong; }
    public void setLatLong(String latLong) { this.latLong = latLong; }

    public String getTechnicianName() { return technicianName; }
    public void setTechnicianName(String technicianName) { this.technicianName = technicianName; }

    public String getTechnicianPhone() { return technicianPhone; }
    public void setTechnicianPhone(String technicianPhone) { this.technicianPhone = technicianPhone; }

    public String getAssignedAt() { return assignedAt; }
    public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

    public Long getUserId() { 
        if (userId != null) return userId;
        if (user != null) return user.getId();
        return null;
    }
    
    public void setUserId(Long userId) { 
        this.userId = userId;  // Directly set the userId field for persistence
        if (userId != null) {
            User u = new User();
            u.setId(userId);
            this.user = u;
        }
    }

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
