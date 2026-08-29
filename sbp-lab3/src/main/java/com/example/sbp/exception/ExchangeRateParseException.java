package com.example.sbp.exception;

public class ExchangeRateParseException extends RuntimeException {
    public ExchangeRateParseException(String message) {
        super(message);
    }
}