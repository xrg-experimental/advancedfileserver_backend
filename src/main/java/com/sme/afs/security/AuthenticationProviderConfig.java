package com.sme.afs.security;

import com.sme.afs.service.DsmAuthenticationService;
import com.sme.afs.service.OtpService;
import com.sme.afs.service.SessionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthenticationProviderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DsmAuthenticationProvider dsmAuthenticationProvider(
            DsmAuthenticationService dsmAuthenticationService,
            UserDetailsService userDetailsService,
            SessionService sessionService) {
        return new DsmAuthenticationProvider(dsmAuthenticationService, userDetailsService, sessionService);
    }

    @Bean
    public LocalAuthenticationProvider localAuthenticationProvider(
            UserDetailsService userDetailsService,
            SessionService sessionService,
            PasswordEncoder passwordEncoder,
            OtpService otpService) {
        return new LocalAuthenticationProvider(userDetailsService, sessionService, passwordEncoder, otpService);
    }
}
