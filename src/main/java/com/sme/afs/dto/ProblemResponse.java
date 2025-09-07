package com.sme.afs.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        @JsonProperty("correlationId") String correlationId
) {}