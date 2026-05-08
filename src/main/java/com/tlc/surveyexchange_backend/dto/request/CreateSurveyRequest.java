package com.tlc.surveyexchange_backend.dto.request;

import com.tlc.surveyexchange_backend.model.SurveyStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateSurveyRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String category;

    private Integer estimatedTime;

    private Integer targetResponses;

    private SurveyStatus status;

    @NotEmpty(message = "Survey must have at least one question")
    @Valid
    private List<QuestionRequest> questions;
}
