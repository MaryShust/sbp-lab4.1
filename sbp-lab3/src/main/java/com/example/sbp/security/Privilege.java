package com.example.sbp.security;

public enum Privilege {
    // Account
    ACCOUNT_CREATE,
    ACCOUNT_SUPER_READ,
    ACCOUNT_READ,
    ACCOUNT_READ_BY_PHONE,
    ACCOUNT_ACTIVATE,

    // Bill
    BILL_CREATE,
    BILL_SUPER_READ,
    BILL_READ,
    BILL_REPLENISH,
    BILL_READ_DEFAULT,

    // Payment
    PAYMENT_CREATE,
    PAYMENT_SUPER_READ_STATUS,
    PAYMENT_READ_STATUS,

    // Admin only
    USER_MANAGE_ROLES
}