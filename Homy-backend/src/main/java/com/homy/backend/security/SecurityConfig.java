package com.homy.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // ✅ PUBLIC ENDPOINTS
                .requestMatchers(
                    "/api/auth/**",
                    "/h2-console/**",
                    "/api/contacts",

                    // allow public booking creation
                    // POST /api/bookings should be open to public so customers can submit bookings
                    // while other booking endpoints remain protected for admin
                    

                    // ✅ SWAGGER
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // Allow unauthenticated POST to create bookings
                .requestMatchers(HttpMethod.POST, "/api/bookings").permitAll()
                // Allow preflight OPTIONS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Allow unauthenticated POST to submit contact messages
                .requestMatchers(HttpMethod.POST, "/api/contacts").permitAll()

                // TEMP DEBUG: allow all requests to verify 403 cause
                .anyRequest().permitAll()
            )
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // For H2 console
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow the dev frontend origin and the production frontend origin on Railway.
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:4200",
            "https://homy-sofa-frontend.onrender.com",
            "homy-sofa.netlify.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*", "Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
