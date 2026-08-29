package com.example.sbp.exception;

public class PhoneNumberFormatException extends RuntimeException {
    public PhoneNumberFormatException(String message) {
        super(message);
    }
}