package com.tlc.surveyexchange_backend.exception;

public class InsufficientCreditsException extends RuntimeException {

    private final int currentBalance;

    public InsufficientCreditsException(int currentBalance) {
        super("Insufficient credits. Required: 5, available: " + currentBalance);
        this.currentBalance = currentBalance;
    }

    public int getCurrentBalance() {
        return currentBalance;
    }
}
