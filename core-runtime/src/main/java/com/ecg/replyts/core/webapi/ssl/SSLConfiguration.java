package com.ecg.replyts.core.webapi.ssl;

import com.google.common.base.Preconditions;
import org.springframework.core.env.Environment;

import java.util.Optional;

public final class SSLConfiguration {

    private final int httpsPort;
    final static String NEXT_PROTOCOL = "http/1.1";
    private final int bufferSize;
    private final long idleTimeoutMs;
    private final boolean needClientAuth;
    private final boolean allowHttp;
    private final KeystoreBasedSSLConfiguration keystoreBasedSSLConfiguration;
    private final PemBasedSSLConfiguration pemBasedSSLConfiguration;

    public static SSLConfiguration createSSLConfiguration(Environment properties) {
        return new SSLConfiguration(
                properties.getProperty("replyts.ssl.store.format", "keystore"),
                properties.getProperty("replyts.pem.key.location"),
                properties.getProperty("replyts.pem.crt.location"),
                properties.getProperty("replyts.pem.password", ""),
                properties.getProperty("replyts.truststore.location"),
                properties.getProperty("replyts.truststore.password"),
                properties.getProperty("replyts.truststore.type", "jks"),
                properties.getProperty("replyts.keystore.location"),
                properties.getProperty("replyts.keystore.password"),
                properties.getProperty("replyts.keystore.type", "jks"),
                Integer.valueOf(properties.getProperty("replyts.ssl.port", "443")),
                Integer.valueOf(properties.getProperty("replyts.ssl.buffersize.bytes", "32678")),
                Long.valueOf(properties.getProperty("replyts.ssl.idletimeout.milliseconds", "60000")),
                Boolean.valueOf(properties.getProperty("replyts.ssl.needclientauth", "false")),
                Boolean.valueOf(properties.getProperty("replyts.ssl.allow.http", "false"))
        );
    }

    private SSLConfiguration(String storeFormat, String pemKeyFile, String pemCrtFile, String pemPassword, String trustStoreLocation,
                             String trustStorePassword, String trustStoreType, String keyStoreLocation, String keyStorePassword,
                             String keyStoreType, int httpsPort, int bufferSize, long idleTimeoutMs, boolean needClientAuth, boolean allowHttp) {
        this.httpsPort = httpsPort;
        this.bufferSize = bufferSize;
        this.idleTimeoutMs = idleTimeoutMs;
        this.needClientAuth = needClientAuth;
        this.allowHttp = allowHttp;

        if ("pem".equalsIgnoreCase(storeFormat)) {
            this.pemBasedSSLConfiguration = new PemBasedSSLConfiguration(pemKeyFile, pemCrtFile, pemPassword);
            this.keystoreBasedSSLConfiguration = null;
        } else if ("keystore".equalsIgnoreCase(storeFormat)) {
            this.keystoreBasedSSLConfiguration = new KeystoreBasedSSLConfiguration(trustStoreLocation, trustStorePassword,
                    trustStoreType, keyStoreLocation, keyStorePassword, keyStoreType);
            this.pemBasedSSLConfiguration = null;
        } else {
            throw new IllegalArgumentException("Only pem and keystore are supported ssl store formats");
        }
    }

    int getHttpsPort() {
        return httpsPort;
    }

    int getBufferSize() {
        return bufferSize;
    }

    long getIdleTimeoutMs() {
        return idleTimeoutMs;
    }

    boolean isNeedClientAuth() {
        return needClientAuth;
    }

    Boolean isAllowHttp() {
        return allowHttp;
    }

    Optional<KeystoreBasedSSLConfiguration> getKeystoreBasedSSLConfiguration() {
        return Optional.ofNullable(keystoreBasedSSLConfiguration);
    }

    Optional<PemBasedSSLConfiguration> getPemBasedSSLConfiguration() {
        return Optional.ofNullable(pemBasedSSLConfiguration);
    }

    static class KeystoreBasedSSLConfiguration {

        private final String keyStoreLocation;
        private final String keyStorePassword;
        private final String keyStoreType;
        private final String trustStoreLocation;
        private final String trustStorePassword;
        private final String trustStoreType;

        KeystoreBasedSSLConfiguration(String trustStoreLocation, String trustStorePassword, String trustStoreType,
                                      String keyStoreLocation, String keyStorePassword, String keyStoreType) {
            Preconditions.checkNotNull(trustStoreLocation);
            Preconditions.checkNotNull(trustStorePassword);
            Preconditions.checkNotNull(trustStoreType);
            Preconditions.checkNotNull(keyStoreLocation);
            Preconditions.checkNotNull(keyStorePassword);
            Preconditions.checkNotNull(keyStoreType);

            this.keyStoreLocation = keyStoreLocation;
            this.keyStorePassword = keyStorePassword;
            this.keyStoreType = keyStoreType;
            this.trustStoreLocation = trustStoreLocation;
            this.trustStorePassword = trustStorePassword;
            this.trustStoreType = trustStoreType;
        }

        String getKeyStoreLocation() {
            return keyStoreLocation;
        }

        String getKeyStorePassword() {
            return keyStorePassword;
        }

        String getKeyStoreType() {
            return keyStoreType;
        }

        String getTrustStoreLocation() {
            return trustStoreLocation;
        }

        String getTrustStorePassword() {
            return trustStorePassword;
        }

        String getTrustStoreType() {
            return trustStoreType;
        }
    }

    static class PemBasedSSLConfiguration {
        private final String pemPassword;
        private final String pemKeyFile;
        private final String pemCrtFile;

        PemBasedSSLConfiguration(String pemKeyFile, String pemCrtFile, String pemPassword) {
            Preconditions.checkNotNull(pemKeyFile);
            Preconditions.checkNotNull(pemCrtFile);
            Preconditions.checkNotNull(pemPassword);

            this.pemKeyFile = pemKeyFile;
            this.pemCrtFile = pemCrtFile;
            this.pemPassword = pemPassword;
        }

        String getPemPassword() {
            return pemPassword;
        }

        String getPemKeyFile() {
            return pemKeyFile;
        }

        String getPemCrtFile() {
            return pemCrtFile;
        }
    }
}

