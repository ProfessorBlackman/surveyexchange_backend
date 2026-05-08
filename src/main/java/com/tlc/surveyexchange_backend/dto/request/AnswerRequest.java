package com.tlc.surveyexchange_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AnswerRequest {

    @NotNull(message = "Question ID is required")
    private UUID questionId;

    private String answerValue;
}
