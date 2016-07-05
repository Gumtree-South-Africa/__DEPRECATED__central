package com.ecg.de.ebayk.messagecenter.pushmessage.send.discovery;

/**
 * A convention-based dictionary for service discovery. Usage of
 * an enum is prefered over free-form strings to reduce the chances of errors
 * and increase the compliance of convention.
 */
public enum ServiceName {
    SEND_API;

    /**
     * Get the service name of this service. Conventionally, this will
     * be a lower-case string containing only hyphens.
     */
    public String asServiceName() {
        return name().toLowerCase().replaceAll("_", "-");
    }
}
