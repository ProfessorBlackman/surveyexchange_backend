package com.tlc.surveyexchange_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tlc.surveyexchange_backend.model.QuestionType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionAnalyticsDTO {
    private UUID questionId;
    private String text;
    private QuestionType type;
    private List<OptionDistributionDTO> distribution;
    private List<String> textResponses;
}
