package com.tlc.surveyexchange_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyResponse {

    private UUID id;
    private String title;
    private String description;
    private String category;
    private Integer estimatedTime;
    private Integer targetResponses;
    private LocalDateTime createdAt;
    private LocalDateTime boostExpiry;
    private boolean boosted;
    private int creditValue;
    private UserSummaryResponse owner;
    private List<QuestionResponse> questions;
}
