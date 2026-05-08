package com.tlc.surveyexchange_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tlc.surveyexchange_backend.model.SurveyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveySummaryResponse {

    private UUID id;
    private String title;
    private String category;
    private Integer estimatedTime;
    private Integer targetResponses;
    private int actualResponseCount;
    private SurveyStatus status;
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant publishedAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant boostExpiresAt;
    private boolean boosted;
    private int creditValue;
    private UserSummaryResponse owner;
}
