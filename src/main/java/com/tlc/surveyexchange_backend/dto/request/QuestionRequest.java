package com.tlc.surveyexchange_backend.dto.request;

import com.tlc.surveyexchange_backend.model.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {

    @NotBlank(message = "Question text is required")
    private String text;

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    private List<String> options;

    private boolean required = true;

    private int orderIndex;
}
