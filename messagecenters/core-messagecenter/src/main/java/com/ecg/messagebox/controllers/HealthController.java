package com.ecg.messagebox.controllers;

import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class HealthController {

    @GetMapping("/health")
    public ResponseObject<ObjectNode> health() {
        ObjectNode health = ObjectMapperConfigurer.getObjectMapper()
                .createObjectNode()
                .put("model", "messagebox")
                .put("status", "OK");

        return ResponseObject.of(health);
    }
}