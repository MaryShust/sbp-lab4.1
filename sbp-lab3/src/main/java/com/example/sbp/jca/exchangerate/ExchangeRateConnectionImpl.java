package com.example.sbp.jca.exchangerate;

import java.io.IOException;
import com.example.sbp.jca.ExchangeRateConnection;

public class ExchangeRateConnectionImpl implements ExchangeRateConnection {

    private final ExchangeRateManagedConnection managedConnection;
    private volatile boolean closed;

    public ExchangeRateConnectionImpl(ExchangeRateManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
    }

    @Override
    public String getExchangeRate(String baseCurrency) {
        if (closed) {
            throw new IllegalStateException("Соединение закрыто");
        }
        try {
            return managedConnection.fetchRates(baseCurrency);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Не удалось получить обменный курс", e);
        }
    }

    @Override
    public void close() {
        closed = true;
    }
}