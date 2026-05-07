package com.tlc.surveyexchange_backend.exception;

public class InsufficientCreditsException extends RuntimeException {

    public InsufficientCreditsException(int currentBalance) {
        super("Insufficient credits. Required: 5, available: " + currentBalance);
    }
}
