package com.sme.afs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "session")
public class SessionProperties {
    private int timeout;
    private int maxConcurrent;
    private int cleanupInterval;
    private int refreshWindow;
}
