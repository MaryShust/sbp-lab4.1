package com.example.sbp.jca.bitrix24;

import com.example.sbp.jca.Bitrix24Connection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Bitrix24ConnectionImpl implements Bitrix24Connection {

    private final Bitrix24ManagedConnection managedConnection;
    private final ObjectMapper objectMapper;
    private volatile boolean closed;

    public Bitrix24ConnectionImpl(Bitrix24ManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Long createDeal(String title, String contactName, String phone, String amount, String riskLevel, String description) {
        if (closed) {
            throw new IllegalStateException("Соединение закрыто");
        }
        try {
            Map<String, Object> phoneData = new HashMap<>();
            phoneData.put("VALUE", phone != null ? phone : "");
            phoneData.put("VALUE_TYPE", "WORK");
            
            Map<String, Object> contactFieldsMap = new HashMap<>();
            contactFieldsMap.put("NAME", contactName != null ? contactName : "Неизвестный");
            contactFieldsMap.put("OPENED", "Y");
            if (phone != null) {
                Map<String, Object> phoneWrapper = new HashMap<>();
                phoneWrapper.put("0", phoneData);
                contactFieldsMap.put("PHONE", phoneWrapper);
            }
            
            Map<String, Object> contactFields = new HashMap<>();
            contactFields.put("fields", contactFieldsMap);
            
            String contactResponse = managedConnection.fetchFromBitrix("crm.contact.add", contactFields);
            JsonNode contactJson = objectMapper.readTree(contactResponse);
            Long contactId = contactJson.get("result").asLong();

            Map<String, Object> dealFields = new HashMap<>();
            Map<String, Object> fields = new HashMap<>();
            fields.put("TITLE", title);
            fields.put("CONTACT_ID", contactId);
            fields.put("OPENED", "Y");
            fields.put("STAGE_ID", getStageByRiskLevel(riskLevel));
            
            if (amount != null && !amount.isEmpty()) {
                try {
                    fields.put("OPPORTUNITY", Double.parseDouble(amount));
                } catch (NumberFormatException ignored) {}
            }
            
            fields.put("COMMENTS", "⚠️ Дроппер обнаружен!\n\n" +
                    "Уровень риска: " + riskLevel + "\n" +
                    "Описание: " + (description != null ? description : "Подозрительная активность"));
            
            dealFields.put("fields", fields);
            
            String dealResponse = managedConnection.fetchFromBitrix("crm.deal.add", dealFields);
            JsonNode dealJson = objectMapper.readTree(dealResponse);
            return dealJson.get("result").asLong();

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Не удалось создать сделку в Bitrix24", e);
        }
    }

    private String getStageByRiskLevel(String riskLevel) {
        if (riskLevel == null) return "NEW";
        return switch (riskLevel.toUpperCase()) {
            case "CRITICAL" -> "1";
            case "HIGH" -> "2";
            case "MEDIUM" -> "3";
            default -> "4";
        };
    }

    @Override
    public void close() {
        closed = true;
    }
}
