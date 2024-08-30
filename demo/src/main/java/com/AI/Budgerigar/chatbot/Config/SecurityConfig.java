package com.AI.Budgerigar.chatbot.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF if not needed
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Apply CORS configuration
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login**", "/css/**", "/js/**", "/img/**").permitAll() // Allow specific paths
                        .anyRequest().authenticated() // All other requests require authentication
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login") // Custom login page
                        .defaultSuccessUrl("/chat", true) // Redirect after successful login
                        .failureUrl("/login?error=true") // Redirect after login failure
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/").permitAll() // Redirect after logout
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // Allow all origins or specify domains
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Allowed HTTP methods
        configuration.setAllowedHeaders(List.of("*")); // Allow all headers
        configuration.setAllowCredentials(true); // If cookies need to be included

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply to all endpoints
        return source;
    }
}
