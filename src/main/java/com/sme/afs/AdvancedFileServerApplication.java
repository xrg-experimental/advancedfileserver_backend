package com.sme.afs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
public class AdvancedFileServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdvancedFileServerApplication.class, args);
    }
}
