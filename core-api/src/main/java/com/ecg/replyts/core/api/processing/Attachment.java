package com.ecg.replyts.core.api.processing;

import java.util.Arrays;
import java.util.Objects;

public final class Attachment {
    private final String name;
    private final byte[] payload;

    public Attachment(String name, byte[] payload) {
        this.name = name;
        this.payload = payload;
    }

    public String getName() {
        return name;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attachment that = (Attachment) o;
        return Objects.equals(name, that.name) &&
                Arrays.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(name) + Arrays.hashCode(payload);
    }

    @Override
    public String toString() {
        return String.format("Attachment{name='%s'}", name);
    }
}
