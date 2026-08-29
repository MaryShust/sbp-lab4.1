package com.example.sbp.jca.bitrix24;

import com.example.sbp.jca.Bitrix24Connection;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ConnectionManager;
import java.io.Serializable;

public class Bitrix24ConnectionFactoryImpl implements Serializable {

    private final ManagedConnectionFactory managedConnectionFactory;
    private final ConnectionManager connectionManager;

    public Bitrix24ConnectionFactoryImpl(ManagedConnectionFactory managedConnectionFactory) {
        this.managedConnectionFactory = managedConnectionFactory;
        this.connectionManager = null;
    }

    public Bitrix24ConnectionFactoryImpl(ConnectionManager connectionManager, ManagedConnectionFactory managedConnectionFactory) {
        this.connectionManager = connectionManager;
        this.managedConnectionFactory = managedConnectionFactory;
    }

    public Bitrix24Connection getConnection() throws ResourceException {
        if (connectionManager != null) {
            return (Bitrix24Connection) connectionManager.allocateConnection(managedConnectionFactory, null);
        }
        ManagedConnection managedConnection = managedConnectionFactory.createManagedConnection(null, null);
        return (Bitrix24Connection) managedConnection.getConnection(null, null);
    }
}
