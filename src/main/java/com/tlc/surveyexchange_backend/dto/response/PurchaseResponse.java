package com.tlc.surveyexchange_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {

    private int creditsAdded;
    private int newBalance;
    private CreditTransactionResponse transaction;
}
