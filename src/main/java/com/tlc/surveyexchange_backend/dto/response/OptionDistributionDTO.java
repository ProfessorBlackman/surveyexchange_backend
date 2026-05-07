package com.tlc.surveyexchange_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OptionDistributionDTO {
    private String option;
    private long count;
    private double percentage;
}
