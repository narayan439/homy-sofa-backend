package com.homy.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "services")
public class ServiceEntity {

    @Id
    private String id;

    private String name;

    @Column(length = 1024)
    private String description;

    private Double price;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Persist image path under `image_path` column
    @Column(name = "image_path", length = 1024)
    private String imagePath;

    // Transient field to expose imageUrl to JSON without requiring DB migration.
    @Transient
    private String imageUrl;

    public ServiceEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ✅ UPDATED getter & setter
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    // New getters/setters. Keep backward compatibility: prefer imageUrl when available.
    public String getImageUrl() {
        return (imageUrl != null && !imageUrl.isBlank()) ? imageUrl : imagePath;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        // Mirror to imagePath for older code that may still read imagePath
        if (imageUrl != null && !imageUrl.isBlank()) {
            this.imagePath = imageUrl;
        }
    }
}
