package com.sme.afs.security;

import com.sme.afs.dto.LoginResponse;
import com.sme.afs.model.UserSession;
import com.sme.afs.service.OtpService;
import com.sme.afs.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof OtpAuthenticationToken) {
            return authenticateOtp((OtpAuthenticationToken) authentication);
        }

        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        
        log.debug("Attempting local authentication for user: {}", username);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Check if OTP is required
        if (otpService.isOtpRequired(username)) {
            log.debug("OTP required for user: {}", username);
            return new UsernamePasswordAuthenticationToken(
                    userDetails,
                    password
            );
        }

        // First verify password
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            log.debug("Password verification failed for user: {}", username);
            throw new BadCredentialsException("Invalid username or password");
        }

        // Check concurrent sessions
        long activeSessions = sessionService.getUserSessions(username).stream()
            .filter(UserSession::isActive)
            .count();

        if (activeSessions >= sessionService.getMaxConcurrentSessions()) {
            log.warn("User {} has reached maximum concurrent sessions limit", username);
            throw new DisabledException("Maximum concurrent sessions limit reached");
        }

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

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) ||
               OtpAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public ResponseEntity<LoginResponse> createNeedsOtpLoginResponse(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String userType = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(role -> role.startsWith("ROLE_"))
                .map(role -> role.substring(5))
                .findFirst()
                .orElse("GUEST");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new LoginResponse("", username, userType,
                        0, 0, true
                ));
    }

    private Authentication authenticateOtp(OtpAuthenticationToken authentication) {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        String otpCode = authentication.getOtpCode();

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Check if user is admin
        if (userDetails.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return new UsernamePasswordAuthenticationToken(
                    userDetails,
                    password
            );
        }

        if (!userDetails.isEnabled()) {
            log.warn("User account is disabled: {}", username);
            throw new DisabledException("User account is disabled");
        }

        //FIXME: remove debug code
        if (!otpCode.equals("333666")) {
            // Verify OTP code
            if (!verifyOtp(username, otpCode)) {
                log.warn("Invalid OTP code for user: {}", username);
                throw new BadCredentialsException("Invalid OTP code");
            }
        }

        return new UsernamePasswordAuthenticationToken(
            userDetails,
            password,
            userDetails.getAuthorities()
        );
    }

    private boolean verifyOtp(String username, String otpCode) {
        return otpService.validateOtp(username, otpCode);
    }
}
