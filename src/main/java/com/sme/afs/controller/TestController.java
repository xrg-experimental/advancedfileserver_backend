package com.sme.afs.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/admin/test")
    public String adminTest() {
        return "Admin endpoint";
    }

    @GetMapping("/api/internal/test")
    public String internalTest() {
        return "Internal endpoint";
    }

    @GetMapping("/api/external/test")
    public String externalTest() {
        return "External endpoint";
    }
}
