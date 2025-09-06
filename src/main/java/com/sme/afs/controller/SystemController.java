package com.sme.afs.controller;

import com.sme.afs.dto.SystemStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/system")
public class SystemController {

    @GetMapping("/status")
    public ResponseEntity<SystemStatusResponse> getSystemStatus() {
        SystemStatusResponse status = new SystemStatusResponse(
            "Online",  // You might want to add logic to determine the actual status
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        );
        return ResponseEntity.ok(status);
    }
}
