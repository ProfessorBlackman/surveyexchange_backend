package com.tlc.surveyexchange_backend.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class BoostResult {
    private final int newBalance;
    private final Instant boostExpiresAt;
}
