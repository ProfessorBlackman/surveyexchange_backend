package com.tlc.surveyexchange_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SubmitResponseRequest {

    @NotEmpty(message = "Answers list must not be empty")
    @Valid
    private List<AnswerRequest> answers;
}
