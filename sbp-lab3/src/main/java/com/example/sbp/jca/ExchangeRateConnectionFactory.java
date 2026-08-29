package com.example.sbp.jca;

import jakarta.resource.ResourceException;

public interface ExchangeRateConnectionFactory {
    ExchangeRateConnection getConnection() throws ResourceException;
}