package com.ecg.replyts.core.api.util;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class JsonObjects {
    private static ObjectMapper om = null;

    static {
        om = new ObjectMapper();
        om.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        om.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    public static final class Builder {

        private ObjectNode on = getObjectMapper().createObjectNode();

        public Builder attr(String key, Map<String, String> values) {
            Builder child = JsonObjects.builder();
            for (Map.Entry<String, String> keyValuePair : values.entrySet()) {
                child.attr(keyValuePair.getKey(), keyValuePair.getValue());
            }

            return attr(key, child);
        }

        public Builder attr(String key, JsonNode an) {
            on.put(key, an);
            return this;
        }

        public Builder attr(String key, String value) {
            on.put(key, value);
            return this;
        }

        public Builder attr(String key, long value) {
            on.put(key, value);
            return this;
        }

        public Builder attr(String key, double value) {
            on.put(key, value);
            return this;
        }

        public Builder attr(String key, boolean value) {
            on.put(key, value);
            return this;
        }

        public Builder attr(String key, Builder value) {
            on.put(key, value.build());
            return this;
        }

        public ObjectNode build() {
            return on;
        }

        public Builder success() {
            return attr("state", "OK");
        }

        public Builder failure(String message) {
            return attr("state", "FAILURE").attr("message", message);
        }

        public Builder failure(Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getClass()).append(": ").append(e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) {
                sb.append("\n").append(el.toString());
            }
            return failure(e.getClass().getName() + ": " + e.getLocalizedMessage()).attr("details", sb.toString());
        }

        public String toJson() {
            return build().toString();
        }

        public Builder attr(String attachments, List<String> attachmentNames) {
            ArrayNode array = newJsonArray();
            for (String attachmentName : attachmentNames) {
                array.add(attachmentName);
            }

            return attr(attachments, array);
        }
    }

    private JsonObjects() {
    }

    public static ObjectNode newJsonObject() {
        return getObjectMapper().createObjectNode();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static JsonNode parse(String input) {
        try {
            return getObjectMapper().readTree(input);
        } catch (Exception e) {
            throw new RuntimeException("could not parse input", e);
        }
    }


    public static JsonNode parse(InputStream input) {
        try {
            return getObjectMapper().readTree(input);
        } catch (Exception e) {
            throw new RuntimeException("could not parse input", e);
        }
    }

    public static ArrayNode newJsonArray() {
        return getObjectMapper().createArrayNode();
    }


    public static JsonNode newJsonArray(Collection<String> values) {
        ArrayNode array = newJsonArray();

        for (String key : ImmutableList.copyOf(values)) {
            array.add(key);
        }
        return array;
    }

    /**
     * @return singleton {@link ObjectMapper} instance configured for lenient parsing and pretty-print writing.
     */
    public static ObjectMapper getObjectMapper() {
        return om;
    }

    /**
     * @return a new object mapper instance configured for pretty printing.
     */
    public static ObjectMapper newPrettyPrintObjectMapper() {
        ObjectMapper o = new ObjectMapper();
        o.configure(SerializationFeature.INDENT_OUTPUT, true);
        return o;
    }
}
