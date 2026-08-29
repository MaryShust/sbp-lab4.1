package com.example.sbp.exception;

public class BankAccountAlreadyExistsException extends RuntimeException {
    public BankAccountAlreadyExistsException(String message) {
        super(message);
    }
}