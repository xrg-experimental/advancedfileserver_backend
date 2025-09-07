package com.sme.afs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

// Deprecated: consolidated into SecurityConfig to avoid multiple SecurityFilterChain beans causing conflicts.
// package com.sme.afs.config;
//
// import org.springframework.context.annotation.Configuration;
//
// @Configuration
// public class SecurityHeadersConfig {
//     // No beans here; headers are configured in com.sme.afs.security.SecurityConfig
// }