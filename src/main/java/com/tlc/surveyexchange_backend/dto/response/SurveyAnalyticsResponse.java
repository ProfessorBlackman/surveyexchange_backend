package com.tlc.surveyexchange_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SurveyAnalyticsResponse {
    private UUID surveyId;
    private String title;
    private long totalResponses;
    private Integer targetResponses;
    private Double completionPercentage;
    private List<QuestionAnalyticsDTO> questions;
}
