package com.sme.afs.service;

import com.sme.afs.repository.UserOtpSettingsRepository;
import com.sme.afs.repository.UserRepository;
import com.sme.afs.model.UserOtpSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {
    private final UserOtpSettingsRepository otpSettingsRepository;
    private final UserRepository userRepository;
    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;
    private final QrGenerator qrGenerator;
    
    public boolean isOtpRequired(String username) {
        return userRepository.findByUsername(username)
            .flatMap(user -> otpSettingsRepository.findByUserId(user.getId()))
            .map(UserOtpSettings::isRequired)
            .orElse(false);
    }
    
    public boolean validateOtp(String username, String otpCode) {
        return userRepository.findByUsername(username)
            .flatMap(user -> otpSettingsRepository.findByUserId(user.getId()))
            .filter(UserOtpSettings::isOtpEnabled)
            .map(settings -> {
                try {
                    return codeVerifier.isValidCode(settings.getOtpSecret(), otpCode);
                } catch (Exception e) {
                    log.error("Error validating OTP code for user {}: {}", username, e.getMessage());
                    return false;
                }
            })
            .orElse(false);
    }

    public boolean isOtpEnabled(String username) {
        return userRepository.findByUsername(username)
            .flatMap(user -> otpSettingsRepository.findByUserId(user.getId()))
            .map(UserOtpSettings::isOtpEnabled)
            .orElse(false);
    }

    public boolean isAdminUser(String username) {
        return userRepository.findByUsername(username)
            .map(user -> "ADMIN".equals(user.getUserType().toString()))
            .orElse(false);
    }

    public String generateSecretKey() {
        return secretGenerator.generate();
    }

    public String generateQrCodeImageUri(String username, String secretKey) {
        QrData data = new QrData.Builder()
            .label(username)
            .secret(secretKey)
            .issuer("Advanced File Server")
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();

        try {
            return Arrays.toString(qrGenerator.generate(data));
        } catch (QrGenerationException e) {
            log.error("Failed to generate QR code", e);
            return null;
        }
    }
}
