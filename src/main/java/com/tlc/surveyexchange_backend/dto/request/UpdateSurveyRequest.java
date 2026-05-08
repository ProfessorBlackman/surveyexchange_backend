package com.tlc.surveyexchange_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateSurveyRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String category;
    private Integer estimatedTime;
    private Integer targetResponses;

    @Valid
    private List<QuestionRequest> questions;
}
