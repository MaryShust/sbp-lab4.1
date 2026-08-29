package com.example.sbp.jca.exchangerate;

import com.example.sbp.jca.ExchangeRateConnection;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ConnectionManager;
import java.io.Serializable;

public class ExchangeRateConnectionFactory implements Serializable {

    private final ManagedConnectionFactory mcf;
    private final ConnectionManager cm;

    public ExchangeRateConnectionFactory(ManagedConnectionFactory mcf) {
        this.mcf = mcf;
        this.cm = null;
    }

    public ExchangeRateConnectionFactory(ConnectionManager cm, ManagedConnectionFactory mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    public ExchangeRateConnection getConnection() throws ResourceException {
        if (cm != null) {
            return (ExchangeRateConnection) cm.allocateConnection(mcf, null);
        }
        ManagedConnection mc = mcf.createManagedConnection(null, null);
        return (ExchangeRateConnection) mc.getConnection(null, null);
    }

    public ExchangeRateConnection createConnection() throws ResourceException {
        ManagedConnection mc = mcf.createManagedConnection(null, null);
        return (ExchangeRateConnection) mc.getConnection(null, null);
    }
}
