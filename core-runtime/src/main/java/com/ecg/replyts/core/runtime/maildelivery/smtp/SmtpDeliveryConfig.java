package com.ecg.replyts.core.runtime.maildelivery.smtp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;


/**
 * Configures the {@link SmtpMailDeliveryService} by providing all the information necessary
 * to connect and use an SMTP server
 *
 * @author huttar
 */
@Component
public class SmtpDeliveryConfig implements Serializable {
    private static final long serialVersionUID = 2L;

    @Value("${delivery.smtp.host}")
    private String host;

    @Value("${delivery.smtp.username:}")
    private String username;

    @Value("${delivery.smtp.password:}")
    private String password;

    @Value("${delivery.smtp.port:25}")
    private int port;

    @Value("${delivery.smtp.timeout.connect.ms:10000}")
    private int connectTimeoutInMs;

    @Value("${delivery.smtp.timeout.read.ms:10000}")
    private int readTimeoutInMs;

    @Value("${delivery.smtp.timeout.write.ms:10000}")
    private int writeTimeoutInMs;

    /**
     * @return the SMTP server's host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return username for logging in to smtp
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return password for smtp server
     */
    public String getPassword() {
        return password;
    }

    public boolean isLoginSpecified() {
        return username != null && !username.isEmpty();
    }

    public int getPort() {
        return port;
    }

    public int getConnectTimeoutInMs() {
        return connectTimeoutInMs;
    }

    public int getReadTimeoutInMs() {
        return readTimeoutInMs;
    }

    public int getWriteTimeoutInMs() {
        return writeTimeoutInMs;
    }
}
