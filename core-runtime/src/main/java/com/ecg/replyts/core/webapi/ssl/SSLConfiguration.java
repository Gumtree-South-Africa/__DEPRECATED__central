package com.ecg.replyts.core.webapi.ssl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.Properties;

public final class SSLConfiguration {

    private final int httpsPort;
    private final String protocol;
    private final int bufferSize;
    private final long idleTimeout;
    private final boolean needClientAuth;
    private final String storeFormat;
    private final boolean allowHttp;
    private final Optional<KeystoreBasedSSLConfiguration> keystoreBasedSSLConfiguration;
    private final Optional<PemBasedSSLConfiguration> pemBasedSSLConfiguration;

    public static SSLConfiguration createSSLConfiguration(final Properties properties) {
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
                properties.getProperty("replyts.ssl.protocol", "http/1.1"),
                Long.valueOf(properties.getProperty("replyts.ssl.idletimeout.milliseconds", "60000")),
                Boolean.valueOf(properties.getProperty("replyts.ssl.needclientauth", "false")),
                Boolean.valueOf(properties.getProperty("replyts.ssl.allow.http", "false"))
        );
    }

    public SSLConfiguration(String storeFormat,
                            String pemKeyFile,
                            String pemCrtFile,
                            String pemPassword,
                            String trustStoreLocation,
                            String trustStorePassword,
                            String trustStoreType,
                            String keyStoreLocation,
                            String keyStorePassword,
                            String keyStoreType,
                            int httpsPort,
                            int bufferSize,
                            String protocol,
                            long idleTimeout,
                            boolean needClientAuth,
                            boolean allowHttp) {
        this.httpsPort = httpsPort;
        this.protocol = protocol;
        this.bufferSize = bufferSize;
        this.idleTimeout = idleTimeout;
        this.needClientAuth = needClientAuth;
        this.storeFormat = storeFormat;
        this.allowHttp = allowHttp;

        if("pem".equalsIgnoreCase(this.storeFormat)) {
            this.pemBasedSSLConfiguration = Optional.of(new PemBasedSSLConfiguration(pemKeyFile, pemCrtFile, pemPassword));
            this.keystoreBasedSSLConfiguration = Optional.absent();
        } else if("keystore".equalsIgnoreCase(this.storeFormat)) {
            KeystoreBasedSSLConfiguration keystoreBasedSSLConfiguration = new KeystoreBasedSSLConfiguration(
                    trustStoreLocation,
                    trustStorePassword,
                    trustStoreType,
                    keyStoreLocation,
                    keyStorePassword,
                    keyStoreType
            );
            this.keystoreBasedSSLConfiguration = Optional.of(keystoreBasedSSLConfiguration);
            this.pemBasedSSLConfiguration = Optional.absent();
        } else {
            throw new IllegalArgumentException("Only pem and keystore are supported ssl store formats");
        }
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public boolean isNeedClientAuth() {
        return needClientAuth;
    }

    public String getStoreFormat() {
        return storeFormat;
    }

    public Boolean isAllowHttp() {
        return allowHttp;
    }

    public Optional<KeystoreBasedSSLConfiguration> getKeystoreBasedSSLConfiguration() {
        return keystoreBasedSSLConfiguration;
    }

    public Optional<PemBasedSSLConfiguration> getPemBasedSSLConfiguration() {
        return pemBasedSSLConfiguration;
    }

    public static class KeystoreBasedSSLConfiguration {

        private final String keyStoreLocation;
        private final String keyStorePassword;
        private final String keyStoreType;
        private final String trustStoreLocation;
        private final String trustStorePassword;
        private final String trustStoreType;

        public KeystoreBasedSSLConfiguration(
                String trustStoreLocation, String trustStorePassword, String trustStoreType,
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

        public String getKeyStoreLocation() {
            return keyStoreLocation;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        public String getKeyStoreType() {
            return keyStoreType;
        }

        public String getTrustStoreLocation() {
            return trustStoreLocation;
        }

        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        public String getTrustStoreType() {
            return trustStoreType;
        }
    }

    public static class PemBasedSSLConfiguration {

        private final String pemPassword;
        private final String pemKeyFile;
        private final String pemCrtFile;

        public PemBasedSSLConfiguration(String pemKeyFile, String pemCrtFile, String pemPassword) {
            Preconditions.checkNotNull(pemKeyFile);
            Preconditions.checkNotNull(pemCrtFile);
            Preconditions.checkNotNull(pemPassword);

            this.pemKeyFile = pemKeyFile;
            this.pemCrtFile = pemCrtFile;
            this.pemPassword = pemPassword;
        }

        public String getPemPassword() {
            return pemPassword;
        }

        public String getPemKeyFile() {
            return pemKeyFile;
        }

        public String getPemCrtFile() {
            return pemCrtFile;
        }
    }
}

