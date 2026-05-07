package com.tlc.surveyexchange_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tlc.surveyexchange_backend.model.TransactionType;
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
public class CreditTransactionResponse {

    private UUID id;
    private int amount;
    private TransactionType type;
    private String description;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID referenceId;

    private LocalDateTime createdAt;
}
