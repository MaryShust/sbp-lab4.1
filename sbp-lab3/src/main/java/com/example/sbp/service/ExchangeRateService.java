package com.example.sbp.service;

import com.example.sbp.exception.ExchangeRateNotFoundException;
import com.example.sbp.exception.ExchangeRateParseException;
import com.example.sbp.jca.ExchangeRateConnection;
import com.example.sbp.jca.exchangerate.ExchangeRateConnectionFactoryImpl;
import com.example.sbp.jca.exchangerate.ExchangeRateManagedConnectionFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.resource.ResourceException;
import org.springframework.stereotype.Service;
import java.io.Serializable;

@Service
public class ExchangeRateService implements Serializable {

    private transient ExchangeRateConnectionFactoryImpl connectionFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            ExchangeRateManagedConnectionFactory mcf = new ExchangeRateManagedConnectionFactory();
            connectionFactory = (ExchangeRateConnectionFactoryImpl) mcf.createConnectionFactory();
        } catch (ResourceException e) {
            throw new RuntimeException("Не удалось инициализировать фабрику соединений JCA.", e);
        }
    }

    public Double getRate(String base, String target) {
        try {
            ExchangeRateConnection conn = connectionFactory.getConnection();
            String json = conn.getExchangeRate(base);
            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode rates = root.get("rates");
                if (rates != null && rates.has(target.toUpperCase())) {
                    return rates.get(target.toUpperCase()).asDouble();
                } else {
                    throw new ExchangeRateNotFoundException("Валюта не найдена");
                }
            } catch (JsonProcessingException e) {
                throw new ExchangeRateParseException("Не удалось получить обменный курс");
            }
            finally {
                conn.close();
            }
        } catch (ResourceException e) {
            throw new ExchangeRateParseException("Не удалось получить обменный курс");
        }
    }
}
