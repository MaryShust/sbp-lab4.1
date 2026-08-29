package com.example.sbp.exception;

public class BillInactiveException extends RuntimeException {
    public BillInactiveException(String message) {
        super(message);
    }
}