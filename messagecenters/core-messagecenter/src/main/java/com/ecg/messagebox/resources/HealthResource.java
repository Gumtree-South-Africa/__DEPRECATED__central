package com.ecg.messagebox.resources;

import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthResource {

    private static final ObjectNode HEALTH =
            ObjectMapperConfigurer.getObjectMapper()
                    .createObjectNode()
                    .put("model", "messagebox")
                    .put("status", "OK");

    @ApiOperation(value = "Health check", notes = "Returns status code 200 if the MessageBox is UP and RUNNING")
    @ApiResponses(@ApiResponse(code = 200, message = "Success"))
    @GetMapping("/health")
    public ObjectNode health() {
        return HEALTH;
    }
}