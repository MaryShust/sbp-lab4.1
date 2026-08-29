package com.example.sbp.jca.exchangerate;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.*;
import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.util.Set;

public class ExchangeRateManagedConnectionFactory implements ManagedConnectionFactory {

    private PrintWriter logWriter;

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return new ExchangeRateConnectionFactoryImpl(this);
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cm) {
        return new ExchangeRateConnectionFactoryImpl(cm, this);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxrInfo) {
        return new ExchangeRateManagedConnection();
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxrInfo) {
        for (Object obj : connectionSet) {
            if (obj instanceof ExchangeRateManagedConnection) {
                return (ManagedConnection) obj;
            }
        }
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }
}