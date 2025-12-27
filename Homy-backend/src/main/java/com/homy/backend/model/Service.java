package com.homy.backend.model;

// Legacy POJO kept for compatibility; not a JPA entity anymore.
public class Service {
    private String id;
    private String name;
    private String description;
    private Double price;
    private Boolean isActive = true;

    public Service() {}

    public Service(String id, String name, String description, Double price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.isActive = true;
    }

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
}
