package com.example.sbp.jca.exchangerate;

import jakarta.resource.spi.ManagedConnectionMetaData;

public class ExchangeRateManagedConnectionMetaData implements ManagedConnectionMetaData {

    @Override
    public String getEISProductName() {
        return "ExchangeRateAPI";
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
        return null;
    }
}