package com.tlc.surveyexchange_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ResponseRowDTO {
    private UUID responseId;
    private LocalDateTime submittedAt;
    private List<AnswerRowDTO> answers;
}
