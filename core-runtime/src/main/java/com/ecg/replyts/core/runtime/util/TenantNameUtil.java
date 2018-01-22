package com.ecg.replyts.core.runtime.util;

import java.util.HashMap;
import java.util.Map;

public class TenantNameUtil {
    private static Map<String, String> tenantNames = new HashMap<>();

    static {
        tenantNames.put("ebayk", "ek");
        tenantNames.put("mp", "mp");
        tenantNames.put("dk", "dk");
        tenantNames.put("it", "it");
        tenantNames.put("mde", "mo");
        tenantNames.put("kjca", "ca");
        tenantNames.put("gtau", "au");
        tenantNames.put("gtuk", "uk");
    }

    public static String getShortName(final String longName) {
        return tenantNames.computeIfAbsent(longName, s -> {
            throw new IllegalArgumentException("No such tenant: " + s);
        });
    }
}
