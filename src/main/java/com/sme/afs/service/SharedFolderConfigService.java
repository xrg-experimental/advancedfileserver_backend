package com.sme.afs.service;

import com.sme.afs.config.SharedFolderProperties;
import com.sme.afs.exception.AfsException;
import com.sme.afs.model.SharedFolderConfig;
import com.sme.afs.model.SharedFolderValidation;
import com.sme.afs.model.User;
import com.sme.afs.repository.SharedFolderConfigRepository;
import com.sme.afs.repository.SharedFolderValidationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    public void initializeFromProperties(User systemUser) {
        try {
            // Pre-validate configuration
            validator.validateConfiguration();
            
            // Begin transaction for configuration updates
            configRepository.findByIsBasePath(true).forEach(config -> {
                config.setBasePath(false);
                configRepository.save(config);
            });

            configRepository.findByIsTempPath(true).ifPresent(config -> {
                config.setTempPath(false);
                configRepository.save(config);
            });

            // Add base paths from properties
            for (String path : properties.getBasePaths()) {
                createOrUpdateConfig(path, true, false, systemUser);
            }

            // Add temp path from properties if configured
            String tempPath = properties.getTempPath();
            if (tempPath != null && !tempPath.isEmpty()) {
                createOrUpdateConfig(tempPath, false, true, systemUser);
            }

            // Final validation of saved configuration
            validator.validateConfiguration();
            
            log.info("Successfully initialized {} base paths and temp path configuration", 
                properties.getBasePaths().size());
                
        } catch (Exception e) {
            log.error("Failed to initialize configuration from properties", e);
            throw new AfsException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to initialize configuration: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public void validateAllConfigurations() {
        List<SharedFolderConfig> configs = configRepository.findAll();
        List<String> errors = new ArrayList<>();
        
        for (SharedFolderConfig config : configs) {
            try {
                validator.validatePath(config.getPath(), 
                    config.isBasePath() ? "Base path" : "Temp path");
            } catch (AfsException e) {
                errors.add(String.format("Invalid path %s: %s", 
                    config.getPath(), e.getMessage()));
            }
        }
        
        if (!errors.isEmpty()) {
            throw new AfsException(HttpStatus.BAD_REQUEST, 
                "Configuration validation failed:\n" + String.join("\n", errors));
        }
    }

    @Transactional
    public SharedFolderConfig createOrUpdateConfig(String path, boolean isBasePath, boolean isTempPath, User user) {
        // Normalize path before validation
        Path normalizedPath = Path.of(path).normalize().toAbsolutePath();
        String normalizedPathStr = normalizedPath.toString();

        // Validate path uniqueness
        Optional<SharedFolderConfig> existing = configRepository.findByPath(normalizedPathStr);
        if (existing.isPresent()) {
            SharedFolderConfig existingConfig = existing.get();
            if (existingConfig.isBasePath() != isBasePath || existingConfig.isTempPath() != isTempPath) {
                throw new AfsException(HttpStatus.CONFLICT, 
                    "Path already exists with different type: " + normalizedPathStr);
            }
        }

        // Validate path type conflicts
        if (isBasePath && isTempPath) {
            throw new AfsException(HttpStatus.BAD_REQUEST, 
                "Path cannot be both base path and temp path");
        }

        // Check temp path uniqueness and conflicts
        if (isTempPath) {
            Optional<SharedFolderConfig> existingTemp = configRepository.findByIsTempPath(true);
            if (existingTemp.isPresent() && !existingTemp.get().getPath().equals(normalizedPathStr)) {
                throw new AfsException(HttpStatus.CONFLICT, 
                    "Another temp path already exists: " + existingTemp.get().getPath());
            }
        }

        // Validate temp path is not under any base path
        if (isTempPath) {
            List<SharedFolderConfig> basePaths = configRepository.findByIsBasePath(true);
            for (SharedFolderConfig baseConfig : basePaths) {
                if (normalizedPathStr.startsWith(baseConfig.getPath())) {
                    throw new AfsException(HttpStatus.BAD_REQUEST,
                        "Temp path cannot be under a base path: " + baseConfig.getPath());
                }
            }
        }

        // Validate base paths don't contain each other
        if (isBasePath) {
            List<SharedFolderConfig> basePaths = configRepository.findByIsBasePath(true);
            for (SharedFolderConfig baseConfig : basePaths) {
                if (normalizedPathStr.startsWith(baseConfig.getPath()) || 
                    baseConfig.getPath().startsWith(normalizedPathStr)) {
                    throw new AfsException(HttpStatus.BAD_REQUEST,
                        "Base paths cannot contain each other: " + baseConfig.getPath());
                }
            }
        }

        SharedFolderConfig config = existing.orElse(new SharedFolderConfig());

        config.setPath(path);
        config.setBasePath(isBasePath);
        config.setTempPath(isTempPath);
        
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
            SharedFolderValidation pathValidation = validator.validatePath(path, isBasePath ? "Base path" : "Temp path");
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

    public List<SharedFolderConfig> getBasePaths() {
        return configRepository.findByIsBasePath(true);
    }

    public SharedFolderConfig getTempPath() {
        return configRepository.findByIsTempPath(true)
            .orElseThrow(() -> new AfsException(HttpStatus.NOT_FOUND, "No temp path configured"));
    }
}
