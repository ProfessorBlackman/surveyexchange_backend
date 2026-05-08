package com.tlc.surveyexchange_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ResponseRowDTO {
    private UUID responseId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant respondedAt;

    private List<AnswerRowDTO> answers;
}
