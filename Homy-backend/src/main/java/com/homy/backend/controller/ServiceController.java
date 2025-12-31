package com.homy.backend.controller;

import com.homy.backend.model.ServiceEntity;
import com.homy.backend.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "*")
public class ServiceController {

    @Autowired
    private ServiceRepository serviceRepository;

    @Value("${app.upload.dir:uploads/backend}")
    private String uploadDirProp;

    // ===================== GET ALL =====================
    @GetMapping
    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findAll();
    }

    // ===================== GET ACTIVE =====================
    @GetMapping("/active")
    public List<ServiceEntity> getActiveServices() {
        return serviceRepository.findByIsActiveTrue();
    }

    // ===================== GET BY ID =====================
    @GetMapping("/{id}")
    public ResponseEntity<ServiceEntity> getServiceById(@PathVariable String id) {
        return serviceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===================== CREATE =====================
    @PostMapping
    public ResponseEntity<ServiceEntity> createService(
            @RequestBody ServiceEntity service) {

        if (service.getId() == null || service.getId().isBlank()) {
            service.setId("svc-" + System.currentTimeMillis());
        }

        if (service.getCreatedAt() == null) {
            service.setCreatedAt(LocalDateTime.now());
        }

        // If frontend provided imageUrl, ensure entity stores it (mirrors to image_path too)
        if (service.getImageUrl() != null && !service.getImageUrl().isBlank()) {
            service.setImageUrl(service.getImageUrl());
        }

        ServiceEntity saved = serviceRepository.save(service);
        return ResponseEntity.ok(saved);
    }

    // ===================== UPDATE =====================
    @PutMapping("/{id}")
    public ResponseEntity<ServiceEntity> updateService(
            @PathVariable String id,
            @RequestBody ServiceEntity service) {

        Optional<ServiceEntity> optional = serviceRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceEntity existing = optional.get();

        if (service.getName() != null)
            existing.setName(service.getName());

        if (service.getDescription() != null)
            existing.setDescription(service.getDescription());

        if (service.getPrice() != null)
            existing.setPrice(service.getPrice());

        if (service.getIsActive() != null)
            existing.setIsActive(service.getIsActive());

        // Accept either imageUrl or legacy imagePath from client
        if (service.getImageUrl() != null && !service.getImageUrl().isBlank()) {
            existing.setImageUrl(service.getImageUrl());
        } else if (service.getImagePath() != null && !service.getImagePath().isBlank()) {
            existing.setImagePath(service.getImagePath());
        }

        ServiceEntity updated = serviceRepository.save(existing);
        return ResponseEntity.ok(updated);
    }

    // ===================== DELETE =====================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable String id) {
        return serviceRepository.findById(id)
                .map(service -> {
                    serviceRepository.delete(service);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ===================== IMAGE UPLOAD =====================
    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("No file uploaded");
        }

        try {
            String original = StringUtils.cleanPath(file.getOriginalFilename());
            String filename = System.currentTimeMillis() + "_" + original;

            // Resolve configured upload dir (relative -> absolute)
            File directory = new File(uploadDirProp);
            if (!directory.isAbsolute()) {
                directory = new File(System.getProperty("user.dir"), uploadDirProp);
            }
            if (!directory.exists()) directory.mkdirs();

            File destination = new File(directory, filename);

            // Copy stream (more robust than transferTo in some environments)
            try (var in = file.getInputStream(); var out = java.nio.file.Files.newOutputStream(destination.toPath())) {
                in.transferTo(out);
            }

                // Public URL used by frontend, include server base so it works without proxy
                String publicPath = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/assets/backend/")
                    .path(filename)
                    .toUriString();

                return ResponseEntity.ok(publicPath);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Image upload failed: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Image upload failed: " + e.getMessage());
        }
    }
}
