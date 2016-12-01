package com.ecg.de.kleinanzeigen;

/**
 * Created by johndavis on 30/11/16.
 */

import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

public class UUIDSerializer implements Serializer<UUID> {
    private static final Logger LOG = LoggerFactory.getLogger(UUIDSerializer.class);

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, UUID data) {
        if(data == null) {
            return null;
        } else {
            try {
                ByteBuffer bytes = ByteBuffer.allocate(16);
                bytes.putLong(0, data.getMostSignificantBits());
                bytes.putLong(8, data.getLeastSignificantBits());
                return bytes.array();
            } catch (RuntimeException exception) {
                LOG.error("Serialization error", exception);
                throw exception;
            }
        }
    }

    @Override
    public void close() {
    }
}
