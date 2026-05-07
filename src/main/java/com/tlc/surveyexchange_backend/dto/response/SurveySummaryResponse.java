package com.tlc.surveyexchange_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private LocalDateTime createdAt;
    private boolean boosted;
    private int creditValue;
    private UserSummaryResponse owner;
}
