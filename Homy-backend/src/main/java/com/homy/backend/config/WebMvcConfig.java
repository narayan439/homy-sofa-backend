package com.homy.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/backend}")
    private String uploadDirProp;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = uploadDirProp;
        File f = new File(dir);
        if (!f.isAbsolute()) {
            f = new File(System.getProperty("user.dir"), dir);
        }
        // Use URI form (file:///...) to avoid backslash issues on Windows
        String location = f.toPath().toAbsolutePath().toUri().toString();
        if (!location.endsWith(File.separator)) {
            location = location + "/";
        }
        registry.addResourceHandler("/assets/backend/**")
            .addResourceLocations(location);
    }
}
