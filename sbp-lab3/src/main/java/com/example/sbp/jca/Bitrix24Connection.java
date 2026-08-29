package com.example.sbp.jca;

public interface Bitrix24Connection extends AutoCloseable {
    Long createDeal(String title, String contactName, String phone, String amount, String riskLevel, String description);
    @Override
    void close();
}
