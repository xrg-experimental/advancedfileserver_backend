package com.sme.afs.dto;

import lombok.Data;

@Data
public class SystemStatusResponse {
    private String status;  // 'Online' | 'Offline' | 'Degraded'
    private String lastChecked;

    public SystemStatusResponse(String status, String lastChecked) {
        this.status = status;
        this.lastChecked = lastChecked;
    }
}
