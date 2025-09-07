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
            
            log.info("Successfully initialized base path configuration");
                
        } catch (Exception e) {
            log.error("Failed to initialize configuration from properties", e);
            throw new AfsException(ErrorCode.INTERNAL_ERROR,
                    "Failed to initialize configuration", e);
        }
    }

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
}
