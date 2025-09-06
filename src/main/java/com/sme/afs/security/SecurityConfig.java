package com.sme.afs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;
    private final DsmAuthenticationProvider dsmAuthenticationProvider;
    private final LocalAuthenticationProvider localAuthenticationProvider;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            JwtAuthenticationEntryPoint jwtAuthEntryPoint,
            DsmAuthenticationProvider dsmAuthenticationProvider,
            LocalAuthenticationProvider localAuthenticationProvider) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.dsmAuthenticationProvider = dsmAuthenticationProvider;
        this.localAuthenticationProvider = localAuthenticationProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF completely for testing
            .sessionManagement(session
                    -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exc
                    -> exc.authenticationEntryPoint(jwtAuthEntryPoint))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/otp-login").permitAll()
                .requestMatchers("/auth/refresh").permitAll()
                .requestMatchers("/auth/logout").authenticated()
                .requestMatchers("/auth/sessions/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/system/status").permitAll()
                .requestMatchers("/api/files/**").authenticated()
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/internal/**").hasAuthority("ROLE_INTERNAL")
                .requestMatchers("/api/external/**").hasAuthority("ROLE_EXTERNAL")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        http.authenticationProvider(dsmAuthenticationProvider);
        http.authenticationProvider(localAuthenticationProvider);
        
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}
