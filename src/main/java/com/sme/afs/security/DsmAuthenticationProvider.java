package com.sme.afs.security;

import com.sme.afs.model.UserSession;
import com.sme.afs.service.DsmAuthenticationService;
import com.sme.afs.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DsmAuthenticationProvider implements AuthenticationProvider {

    private final DsmAuthenticationService dsmAuthenticationService;
    private final UserDetailsService userDetailsService;
    private final SessionService sessionService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        String otpCode = authentication.getDetails().toString();

        log.debug("Attempting DSM authentication for user: {}", username);

        // Load user details first to check if admin
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (userDetails == null) {
            log.warn("User not found: {}", username);
            throw new BadCredentialsException("Invalid username or password");
        }

        // Only allow admin authentication through DSM
        if (userDetails.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            log.warn("Non-admin user attempting DSM authentication: {}", username);
            throw new BadCredentialsException("Only admin users can authenticate through DSM");
        }

        // Check concurrent sessions
        long activeSessions = sessionService.getUserSessions(username).stream()
                .filter(UserSession::isActive)
                .count();

        if (activeSessions >= sessionService.getMaxConcurrentSessions()) {
            log.warn("User {} has reached maximum concurrent sessions limit", username);
            throw new DisabledException("Maximum concurrent sessions limit reached");
        }

        // Authenticate with DSM
        if (dsmAuthenticationService.authenticate(username, password, otpCode)) {
            if (!userDetails.isEnabled()) {
                log.warn("User account is disabled: {}", username);
                throw new DisabledException("User account is disabled");
            }

            return new UsernamePasswordAuthenticationToken(
                    userDetails,
                    password,
                    userDetails.getAuthorities()
            );
        }

        log.debug("DSM Authentication failed for user: {}", username);
        throw new BadCredentialsException("Invalid username or password");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}