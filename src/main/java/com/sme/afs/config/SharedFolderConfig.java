package com.sme.afs.config;

import com.sme.afs.service.SharedFolderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SharedFolderConfig {

    private final SharedFolderValidator validator;
    private final SharedFolderProperties properties;

    public String getBasePath() {
        return properties.getBasePath();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {
        if (properties.isValidateOnStartup()) {
            log.info("Validating shared folder configuration...");
            validator.validateConfiguration();
            log.info("Shared folder configuration validated successfully");
        }
    }

    @Scheduled(fixedDelayString = "#{@sharedFolderProperties.scanIntervalSeconds * 1000}")
    public void validatePeriodically() {
        if (properties.isValidateOnStartup() && properties.isAllowRuntimeUpdates()) {
            log.debug("Performing periodic shared folder validation");
            validator.validateConfiguration();
        }
    }
}
