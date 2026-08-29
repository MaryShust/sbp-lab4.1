package com.example.sbp.exception;

public class BankBicFormatException extends RuntimeException {
    public BankBicFormatException(String message) {
        super(message);
    }
}