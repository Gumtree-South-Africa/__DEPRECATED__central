package com.ecg.replyts.core.runtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

@Component
public class DataCenterAwareness {
    private static final String HOST_NAME_TEMPLATE = "%s.prod.comaas.cloud";
    private static final String ENVIRONMENT_ENV_VAR = "ENVIRONMENT";
    private static final String CURRENT_DC_ENV_VAR = "NOMAD_REGION";
    private static final String ENV_PROD = "prod";

    @Value("${replyts.tenant.short}")
    private String tenantShort;

    private String getHostName() {
        return String.format(HOST_NAME_TEMPLATE, tenantShort);
    }

    /**
     * @return String the currently active DC
     */
    private String activeDc() {
        java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

        try {
            DirContext dirContext = new InitialDirContext(env);
            Attributes attrs = dirContext.getAttributes(getHostName(), new String[]{"TXT"});
            Attribute attr = attrs.get("TXT");

            if (attr == null) {
                throw new IllegalStateException("Could not determine current DC");
            }

            String txtRecord = attr.get().toString();

            if (!StringUtils.hasText(txtRecord)) {
                throw new IllegalStateException("Could not determine current DC, no TXT record found for " + getHostName());
            }

            String[] split = txtRecord.split("=");
            if (split.length != 2) {
                throw new IllegalStateException("Could not determine current DC, unexpected TXT record '" + txtRecord + "' found for " + getHostName());
            }
            return split[1];
        } catch (NamingException e) {
            throw new IllegalStateException("Could not determine current DC", e);
        }
    }

    /**
     * @return String the DC this allocation is running in
     */
    private String getOwnDc() {
        return System.getenv(CURRENT_DC_ENV_VAR);
    }

    /**
     * @return String the environment this allocation is running in, "lp" or "prod"
     */
    private String getCurrentEnv() {
        return System.getenv(ENVIRONMENT_ENV_VAR);
    }

    public boolean runningInActiveDc() {
        if (!getCurrentEnv().equalsIgnoreCase(ENV_PROD)) {
            return true;
        }
        return getOwnDc().equalsIgnoreCase(activeDc());
    }
}
