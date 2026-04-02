package com.homy.backend.controller;

import com.homy.backend.model.Contact;
import com.homy.backend.repository.ContactRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class ContactController {
    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    private final ContactRepository contactRepository;

    public ContactController(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @PostMapping
    public ResponseEntity<Contact> createContact(@Validated @RequestBody Contact contact) {
        log.info("Received contact submission: {}", contact);
        Contact saved = contactRepository.save(contact);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Contact>> listAll() {
        return ResponseEntity.ok(contactRepository.findAll());
    }
}
