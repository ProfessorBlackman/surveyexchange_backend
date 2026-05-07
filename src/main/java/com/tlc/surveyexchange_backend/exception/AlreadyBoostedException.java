package com.tlc.surveyexchange_backend.exception;

import java.time.Instant;

public class AlreadyBoostedException extends RuntimeException {

    private final Instant expiresAt;

    public AlreadyBoostedException(Instant expiresAt) {
        super("Survey is already boosted");
        this.expiresAt = expiresAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
