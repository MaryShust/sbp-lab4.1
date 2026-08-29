package com.example.sbp.jca.bitrix24;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.*;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

public class Bitrix24ManagedConnection implements ManagedConnection {

    private volatile boolean destroyed;
    private PrintWriter logWriter;
    private final String webhookUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public Bitrix24ManagedConnection(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxrInfo) throws ResourceException {
        if (destroyed) {
            throw new ResourceException("Соединение разорвано");
        }
        return new Bitrix24ConnectionImpl(this);
    }

    @Override
    public void destroy() {
        destroyed = true;
    }

    @Override
    public void cleanup() {}

    @Override
    public void associateConnection(Object connection) {}

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {}

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {}

    @Override
    public ManagedConnectionMetaData getMetaData() {
        return new Bitrix24ManagedConnectionMetaData();
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("getLocalTransaction() не поддерживается");
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("getXAResource() не поддерживается");
    }

    String fetchFromBitrix(String method, Map<String, Object> params) throws IOException, InterruptedException {
        String url = webhookUrl + "/" + method + ".json";
        
        Map<String, Object> requestBody = params != null ? params : Map.of();
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
