package com.sme.afs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "shared-folder")
public class SharedFolderProperties {
    private List<String> basePaths = new ArrayList<>();
    private String tempPath;
    private boolean validateOnStartup = true;
    private int scanIntervalSeconds = 300;
    private boolean allowRuntimeUpdates = true;
    private boolean validatePermissions = true;
    private boolean createMissingDirectories = true;
    private int maxBasePaths = 10;
    private int minBasePaths = 1;
    private String packageOwner;
    private String packageOwnerFull;
    private boolean enforcePackageOwner = true;
    
    // For backward compatibility
    public void setBasePath(String basePath) {
        this.basePaths = new ArrayList<>();
        if (basePath != null) {
            this.basePaths.add(basePath);
        }
    }
    
    public String getBasePath() {
        return basePaths.isEmpty() ? null : basePaths.get(0);
    }
}
