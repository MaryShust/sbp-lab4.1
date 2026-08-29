package com.example.sbp.jca.bitrix24;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.*;
import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.util.Set;

public class Bitrix24ManagedConnectionFactory implements ManagedConnectionFactory {

    private PrintWriter logWriter;
    private String webhookUrl;
    private String webhookId;
    private String webhookHash;

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return new Bitrix24ConnectionFactoryImpl(this);
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cm) {
        return new Bitrix24ConnectionFactoryImpl(cm, this);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxrInfo) {
        return new Bitrix24ManagedConnection(webhookUrl);
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxrInfo) {
        for (Object obj : connectionSet) {
            if (obj instanceof Bitrix24ManagedConnection) {
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

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    public String getWebhookHash() {
        return webhookHash;
    }

    public void setWebhookHash(String webhookHash) {
        this.webhookHash = webhookHash;
    }
}
