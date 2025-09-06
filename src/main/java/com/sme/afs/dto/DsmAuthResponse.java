package com.sme.afs.dto;

import lombok.Data;

@Data
public class DsmAuthResponse {
    private boolean success;
    private Data data;
    private Error error;

    @lombok.Data
    public static class Data {
        private String sid;
    }

    @lombok.Data
    public static class Error {
        private int code;
    }
}
