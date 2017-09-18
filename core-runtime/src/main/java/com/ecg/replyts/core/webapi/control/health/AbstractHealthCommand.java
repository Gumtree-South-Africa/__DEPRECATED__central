package com.ecg.replyts.core.webapi.control.health;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.runtime.util.ArrayNodeCollector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

abstract class AbstractHealthCommand implements HealthCommand {

    static final ArrayNodeCollector ARRAY_NODE_COLLECTOR = new ArrayNodeCollector();
    static final String UNKNOWN = "unknown";
    static final String STATUS = "status";
    static final String NAME = "name";
    static final String DETAIL = "detail";

    enum Status {
        UP, DOWN
    }

    static ObjectNode nameValue(String key, String value) {
        return JsonObjects.builder()
                .attr(NAME, key)
                .attr("value", value)
                .build();
    }

    static ObjectNode error(String descriptions) {
        return JsonObjects.builder()
                .attr("error", descriptions)
                .build();
    }

    static ObjectNode status(Status status) {
        return JsonObjects.builder()
                .attr(STATUS, status.name())
                .build();
    }

    static ObjectNode status(String name, Status status) {
        return JsonObjects.builder()
                .attr(STATUS, status.name())
                .attr(NAME, name)
                .build();
    }

    static ObjectNode status(String name, Status status, String detail) {
        return JsonObjects.builder()
                .attr(NAME, name)
                .attr(STATUS, status.name())
                .attr(DETAIL, detail)
                .build();
    }

    static ObjectNode status(Status status, String detail) {
        return JsonObjects.builder()
                .attr(STATUS, status.name())
                .attr(DETAIL, detail)
                .build();
    }

    static ObjectNode up() {
        return status(Status.UP);
    }

    static ObjectNode down(String detail) {
        return status(Status.DOWN, detail);
    }

    static Status getOverallStatus(ArrayNode connections) {
        for (JsonNode connection : connections) {
            if (Status.valueOf(connection.get("status").asText()) == Status.DOWN) {
                return Status.DOWN;
            }
        }

        return Status.UP;
    }
}
