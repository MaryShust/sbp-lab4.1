package com.example.sbp.exception;

public class BillNotBelongAccountExeption extends RuntimeException{
    public BillNotBelongAccountExeption(String message) {
        super(message);
    }
}
