package com.ecg.messagebox.resources;

import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthResource {

    private static final ObjectNode HEALTH =
            ObjectMapperConfigurer.getObjectMapper()
                    .createObjectNode()
                    .put("model", "messagebox")
                    .put("status", "OK");

    @GetMapping("/health")
    public ObjectNode health() {
        return HEALTH;
    }
}