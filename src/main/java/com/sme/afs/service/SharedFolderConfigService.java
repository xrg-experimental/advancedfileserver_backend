package com.sme.afs.service;

import com.sme.afs.config.SharedFolderProperties;
import com.sme.afs.error.ErrorCode;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.SharedFolderConfig;
import com.sme.afs.model.SharedFolderValidation;
import com.sme.afs.model.User;
import com.sme.afs.repository.SharedFolderConfigRepository;
import com.sme.afs.repository.SharedFolderValidationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedFolderConfigService {
    
    private final SharedFolderConfigRepository configRepository;
    private final SharedFolderValidationRepository validationRepository;
    private final SharedFolderValidator validator;
    private final SharedFolderProperties properties;

    @Transactional
    public SharedFolderConfig createOrUpdateConfig(String path, User user) {
        // Normalize the path before validation
        Path normalizedPath = Path.of(path).normalize().toAbsolutePath();
        String normalizedPathStr = normalizedPath.toString();

        Optional<SharedFolderConfig> existing = configRepository.findByPath(normalizedPathStr);
        SharedFolderConfig config = existing.orElse(new SharedFolderConfig());

        config.setPath(normalizedPathStr);

        LocalDateTime now = LocalDateTime.now();

        if (config.getId() == null) {
            config.setCreatedAt(now);
            config.setCreatedBy(user);
        }

        config.setModifiedAt(now);
        config.setModifiedBy(user);

        config = configRepository.save(config);

        // Create or update validation status
        SharedFolderValidation validation = validationRepository.findByConfigId(config.getId())
            .orElse(new SharedFolderValidation());

        validation.setConfig(config);
        validation.setLastCheckedAt(now);
        validation.setCheckedBy(user);

        try {
            SharedFolderValidation pathValidation = validator.validatePath(normalizedPathStr);
            validation.setValid(pathValidation.isValid());
            validation.setErrorMessage(pathValidation.getErrorMessage());
            validation.setCanRead(pathValidation.getCanRead());
            validation.setCanWrite(pathValidation.getCanWrite());
            validation.setCanExecute(pathValidation.getCanExecute());
            validation.setPermissionCheckError(pathValidation.getPermissionCheckError());
        } catch (AfsException e) {
            validation.setValid(false);
            validation.setErrorMessage(e.getMessage());
            validation.setPermissionCheckError("Validation failed: " + e.getMessage());
        }

        validationRepository.save(validation);

        return config;
    }

    public List<SharedFolderConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    @Transactional
    public void initializeFromProperties(User systemUser) {
        try {
            // Pre-validate configuration
            validator.validateConfiguration();

            // Add the base path from properties
            String basePath = properties.getBasePath();
            if (basePath != null && !basePath.isEmpty()) {
                createOrUpdateConfig(basePath, systemUser);
            }

            // Final validation of saved configuration
            validator.validateConfiguration();

        } catch (Exception e) {
            log.error("Failed to initialize configuration from properties", e);
            throw new AfsException(ErrorCode.INTERNAL_ERROR,
                    "Failed to initialize configuration", e);
        }
    }

    @Transactional
    public List<SharedFolderValidation> revalidateAll() {
        log.info("Starting revalidation of all shared folder configurations");

        List<SharedFolderConfig> allConfigs = configRepository.findAll();
        log.info("Found {} configurations to revalidate", allConfigs.size());

        int successCount = 0;
        int errorCount = 0;

        List<SharedFolderValidation> validations = new ArrayList<>();
        for (SharedFolderConfig config : allConfigs) {
            // Validate the path using the existing validator
            SharedFolderValidation validation = validator.validatePath(config.getPath());
            validation.setLastCheckedAt(LocalDateTime.now());

            try {
                log.debug("Revalidating configuration with path: {}", config.getPath());

                // Save the updated configuration
                configRepository.save(config);

                if (validation.isValid()) {
                    successCount++;
                    log.debug("Successfully revalidated configuration: {}", config.getPath());
                } else {
                    errorCount++;
                    log.warn("Validation failed for configuration {}: {}", config.getPath(), validation.getErrorMessage());
                }
            } catch (Exception e) {
                errorCount++;
                log.error("Error during revalidation of configuration {}: {}", config.getPath(), e.getMessage(), e);

                validation.setValid(false);
                validation.setErrorMessage("Validation error: " + e.getMessage());

                try {
                    configRepository.save(config);
                } catch (Exception saveException) {
                    log.error("Failed to save error state for configuration {}: {}", config.getPath(), saveException.getMessage());
                }
            }

            validations.add(validation);
        }

        log.info("Revalidation completed. Success: {}, Errors: {}, Total: {}",
                successCount, errorCount, allConfigs.size());
        return validations;
    }
}
