package com.example.sbp.jca.bitrix24;

import jakarta.resource.spi.ManagedConnectionMetaData;

public class Bitrix24ManagedConnectionMetaData implements ManagedConnectionMetaData {

    @Override
    public String getEISProductName() {
        return "Bitrix24 CRM";
    }

    @Override
    public String getEISProductVersion() {
        return "1.0";
    }

    @Override
    public int getMaxConnections() {
        return 10;
    }

    @Override
    public String getUserName() {
        return "Webhook";
    }
}
