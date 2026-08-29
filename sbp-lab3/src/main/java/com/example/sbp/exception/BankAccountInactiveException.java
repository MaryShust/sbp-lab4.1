package com.example.sbp.exception;

public class BankAccountInactiveException extends RuntimeException {
    public BankAccountInactiveException(String message) {
        super(message);
    }
}