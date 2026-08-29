package com.example.sbp.jca.exchangerate;

import com.example.sbp.jca.ExchangeRateConnection;
import com.example.sbp.jca.ExchangeRateConnectionFactory;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ConnectionManager;

public class ExchangeRateConnectionFactoryImpl implements ExchangeRateConnectionFactory {

    private final ManagedConnectionFactory managedConnectionFactory;
    private final ConnectionManager connectionManager;

    public ExchangeRateConnectionFactoryImpl(ManagedConnectionFactory managedConnectionFactory) {
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = null;
    }

    public ExchangeRateConnectionFactoryImpl(
            ConnectionManager connectionManager,
            ManagedConnectionFactory managedConnectionFactory
    ) {
        this.connectionManager = connectionManager;
        this.managedConnectionFactory = managedConnectionFactory;
    }

    @Override
    public ExchangeRateConnection getConnection() throws ResourceException {
        if (connectionManager != null) {
            return (ExchangeRateConnection) connectionManager.allocateConnection(managedConnectionFactory, null);
        }
        ManagedConnection managedConnection = managedConnectionFactory.createManagedConnection(null, null);
        return (ExchangeRateConnection) managedConnection.getConnection(null, null);
    }
}