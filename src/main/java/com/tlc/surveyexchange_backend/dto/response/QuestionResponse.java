package com.tlc.surveyexchange_backend.dto.response;

import com.tlc.surveyexchange_backend.model.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private UUID id;
    private String text;
    private QuestionType questionType;
    private List<String> options;
    private boolean required;
    private int orderIndex;
}
