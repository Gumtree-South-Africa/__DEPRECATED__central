package com.ecg.messagebox.controllers;

import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final JacksonAwareObjectMapperConfigurer objectMapperConfigurer;

    public HealthController(JacksonAwareObjectMapperConfigurer objectMapperConfigurer) {
        this.objectMapperConfigurer = objectMapperConfigurer;
    }

    @GetMapping("/health")
    public ResponseObject<ObjectNode> getResponseData() {
        ObjectNode health = objectMapperConfigurer.getObjectMapper()
                .createObjectNode()
                .put("model", "messagebox")
                .put("status", "OK");

        return ResponseObject.of(health);
    }
}