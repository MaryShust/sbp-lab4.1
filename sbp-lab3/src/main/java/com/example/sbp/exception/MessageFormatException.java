package com.example.sbp.exception;

public class MessageFormatException extends RuntimeException {
    public MessageFormatException(String message) {
        super(message);
    }
}