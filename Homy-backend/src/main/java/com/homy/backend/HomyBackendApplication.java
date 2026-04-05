package com.homy.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableAsync  // Enable async processing for background tasks like email sending
@EnableCaching  // Enable method-level caching for query optimization
public class HomyBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(HomyBackendApplication.class, args);
    }
}
