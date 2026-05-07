package com.tlc.surveyexchange_backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseRequest {

    @NotNull(message = "Package size is required")
    @JsonProperty("package")
    private Integer creditPackage;
}
