package com.example.sbp.security.jaas;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.HashMap;

public class JaasConfiguration extends Configuration {
    public static final String APPLICATION_NAME = "SBPApplication";
    private static final String LOGIN_MODULE_CLASS = "com.example.sbp.security.jaas.XmlLoginModule";

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        if (!APPLICATION_NAME.equals(name)) return null;
        return new AppConfigurationEntry[]{
                new AppConfigurationEntry(LOGIN_MODULE_CLASS, AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, new HashMap<>())
        };
    }

    public static void register() {
        Configuration.setConfiguration(new JaasConfiguration());
    }
}
