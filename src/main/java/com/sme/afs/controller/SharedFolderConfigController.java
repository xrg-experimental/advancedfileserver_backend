package com.sme.afs.controller;

import com.sme.afs.dto.SharedFolderConfigRequest;
import com.sme.afs.dto.SharedFolderConfigResponse;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.SharedFolderConfig;
import com.sme.afs.model.SharedFolderValidation;
import com.sme.afs.model.User;
import com.sme.afs.service.SharedFolderConfigService;
import com.sme.afs.service.SharedFolderValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import com.sme.afs.repository.SharedFolderValidationRepository;

@Slf4j
@RestController
@RequiredArgsConstructor 
@RequestMapping("/api/admin/storage")
public class SharedFolderConfigController {

    private final SharedFolderConfigService configService;
    private final SharedFolderValidationRepository validationRepository;
    private final SharedFolderValidator validator;

    @GetMapping
    public ResponseEntity<SharedFolderConfigResponse> getAllConfigs() {
        List<SharedFolderConfig> configs = configService.getAllConfigs();
        return ResponseEntity.ok(SharedFolderConfigResponse.of(configs));
    }

    @PostMapping("/initialize")
    public ResponseEntity<SharedFolderConfigResponse> initializeFromProperties(@AuthenticationPrincipal User user) {
        configService.initializeFromProperties(user);
        return ResponseEntity.ok(SharedFolderConfigResponse.of(configService.getAllConfigs()));
    }

    @GetMapping("/status")
    public ResponseEntity<SharedFolderConfigResponse> getConfigurationStatus() {
        List<SharedFolderValidation> validations = validationRepository.findAll();
        
        // Check if any validations are stale
        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(5);
        boolean hasStaleValidations = validations.stream()
                .anyMatch(v -> v.getLastCheckedAt() == null || v.getLastCheckedAt().isBefore(staleThreshold));

        if (hasStaleValidations) {
            // Revalidate and then return freshly persisted rows
            configService.revalidateAll();
            validations = validationRepository.findAll();
        }
        
        return ResponseEntity.ok(SharedFolderConfigResponse.ofValidations(validations));
    }

    @PostMapping("/reload")
    public ResponseEntity<SharedFolderConfigResponse> reloadConfiguration(@AuthenticationPrincipal User user) {
        try {
            configService.initializeFromProperties(user);
            validator.validateConfiguration();
            return ResponseEntity.ok(SharedFolderConfigResponse.of(
                configService.getAllConfigs(), "Configuration reloaded successfully"));
        } catch (AfsException e) {
            return ResponseEntity.status(e.getStatus())
                .body(SharedFolderConfigResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during configuration reload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(SharedFolderConfigResponse.error("Configuration reload failed: " + e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<SharedFolderConfigResponse> validateConfiguration() {
        try {
            validator.validateConfiguration();
            List<SharedFolderValidation> validations = validationRepository.findAll();
            boolean allValid = validations.stream().allMatch(SharedFolderValidation::isValid);
            
            if (allValid) {
                return ResponseEntity.ok(SharedFolderConfigResponse.ofValidations(
                    validations, "All configurations are valid"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(SharedFolderConfigResponse.ofValidations(validations, "Some configurations are invalid"));
            }
        } catch (Exception e) {
            log.error("Validation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(SharedFolderConfigResponse.error("Validation failed: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<SharedFolderConfigResponse> createConfig(
            @Valid @RequestBody SharedFolderConfigRequest request,
            @AuthenticationPrincipal User user) {
        
        SharedFolderConfig config = configService.createOrUpdateConfig(
            request.getPath(),
                user
        );
        return ResponseEntity.ok(SharedFolderConfigResponse.of(config));
    }
}
