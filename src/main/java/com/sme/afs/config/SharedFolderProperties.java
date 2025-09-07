package com.sme.afs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "shared-folder")
public class SharedFolderProperties {
    private String basePath;
    private boolean validateOnStartup = true;
    private int scanIntervalSeconds = 300;
    private boolean allowRuntimeUpdates = true;
    private boolean validatePermissions = true;
    private boolean createMissingDirectories = true;
    private String packageOwner;
    private String packageOwnerFull;
    private boolean enforcePackageOwner = true;
}
