package com.tlc.surveyexchange_backend.exception;

import lombok.Getter;

import java.time.Instant;

@Getter
public class AlreadyBoostedException extends RuntimeException {

    private final Instant expiresAt;

    public AlreadyBoostedException(Instant expiresAt) {
        super("Survey is already boosted");
        this.expiresAt = expiresAt;
    }

}
