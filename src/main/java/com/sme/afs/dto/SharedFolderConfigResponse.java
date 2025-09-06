package com.sme.afs.dto;

import com.sme.afs.model.SharedFolderConfig;
import com.sme.afs.model.SharedFolderValidation;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Map;

@Data
public class SharedFolderConfigResponse {
    private boolean success;
    private Object data;

    public static SharedFolderConfigResponse of(SharedFolderConfig config) {
        SharedFolderConfigResponse response = new SharedFolderConfigResponse();
        response.setSuccess(true);
        response.setData(config);
        return response;
    }

    public static SharedFolderConfigResponse of(List<SharedFolderConfig> configs) {
        SharedFolderConfigResponse response = new SharedFolderConfigResponse();
        response.setSuccess(true);
        response.setData(configs);
        return response;
    }

    public static SharedFolderConfigResponse of(List<SharedFolderConfig> configs, String message) {
        SharedFolderConfigResponse response = new SharedFolderConfigResponse();
        response.setSuccess(true);
        Map<String, Object> data = new HashMap<>();
        data.put("configs", configs);
        data.put("message", message);
        response.setData(data);
        return response;
    }

    public static SharedFolderConfigResponse ofValidations(List<SharedFolderValidation> validations) {
        SharedFolderConfigResponse response = new SharedFolderConfigResponse();
        response.setSuccess(true);
        Map<String, Object> status = new HashMap<>();
        status.put("validations", validations);
        status.put("lastChecked", validations.stream()
            .map(SharedFolderValidation::getLastCheckedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null));
        status.put("isValid", validations.stream()
            .allMatch(SharedFolderValidation::isValid));
        response.setData(status);
        return response;
    }

    public static SharedFolderConfigResponse ofValidations(List<SharedFolderValidation> validations, String message) {
        SharedFolderConfigResponse response = new SharedFolderConfigResponse();
        response.setSuccess(true);
        Map<String, Object> status = new HashMap<>();
        status.put("validations", validations);
        status.put("message", message);
        status.put("lastChecked", validations.stream()
            .map(SharedFolderValidation::getLastCheckedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null));
        status.put("isValid", validations.stream()
            .allMatch(SharedFolderValidation::isValid));
        response.setData(status);
        return response;
    }

    public static SharedFolderConfigResponse error(String message) {
        SharedFolderConfigResponse response = new SharedFolderConfigResponse();
        response.setSuccess(false);
        response.setData(message);
        return response;
    }
}
