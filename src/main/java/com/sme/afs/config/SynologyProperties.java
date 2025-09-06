package com.sme.afs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "synology")
public class SynologyProperties {
    private String host;
    private int port;
    private String username;
    private String password;
    private String sessionName = "FileStation";
    private String apiVersion = "2";
    private String protocol = "http";
    private boolean verifySsl = false;
}
