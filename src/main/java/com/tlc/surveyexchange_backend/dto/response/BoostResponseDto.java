package com.tlc.surveyexchange_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tlc.surveyexchange_backend.service.BoostResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoostResponseDto {

    private int newBalance;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant boostExpiresAt;

    public static BoostResponseDto from(BoostResult result) {
        return BoostResponseDto.builder()
                .newBalance(result.getNewBalance())
                .boostExpiresAt(result.getBoostExpiresAt())
                .build();
    }
}
