package com.tlc.surveyexchange_backend.dto.request;

import com.tlc.surveyexchange_backend.model.SurveyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatchSurveyRequest {

    @NotNull(message = "Status is required")
    private SurveyStatus status;
}
