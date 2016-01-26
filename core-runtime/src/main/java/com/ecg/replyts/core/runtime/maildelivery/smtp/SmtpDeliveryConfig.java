package com.ecg.replyts.core.runtime.maildelivery.smtp;

import java.io.Serializable;


/**
 * Configures the {@link SmtpMailDeliveryService} by providing all the information necessary
 * to connect and use an SMTP server
 *
 * @author huttar
 */
public class SmtpDeliveryConfig implements Serializable {
    private static final long serialVersionUID = 2L;

    private String host;
    private String username = "";
    private String password = "";
    private int port = 25;
    private int connectTimeoutInMs = 0;
    private int readTimeoutInMs = 0;
    private int writeTimeoutInMs = 0;

    /**
     * @return the SMTP server's host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the SMTP server's host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return username for logging in to smtp
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username username for smtp server
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return password for smtp server
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password password for smtp server
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLoginSpecified() {
        return username != null && !username.isEmpty();
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public int getConnectTimeoutInMs() {
        return connectTimeoutInMs;
    }

    public void setConnectTimeoutInMs(int connectTimeoutInMs) {
        this.connectTimeoutInMs = connectTimeoutInMs;
    }

    public int getReadTimeoutInMs() {
        return readTimeoutInMs;
    }

    public void setReadTimeoutInMs(int readTimeoutInMs) {
        this.readTimeoutInMs = readTimeoutInMs;
    }

    public int getWriteTimeoutInMs() {
        return writeTimeoutInMs;
    }

    public void setWriteTimeoutInMs(int writeTimeoutInMs) {
        this.writeTimeoutInMs = writeTimeoutInMs;
    }
}
