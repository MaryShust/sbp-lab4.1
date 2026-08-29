package com.example.sbp.jca;

public interface ExchangeRateConnection {
    String getExchangeRate(String baseCurrency);
    void close();
}