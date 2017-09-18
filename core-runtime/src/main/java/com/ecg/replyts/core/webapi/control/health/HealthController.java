package com.ecg.replyts.core.webapi.control.health;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("health")
public class HealthController {

    private final HealthCommand infoCommand;

    private final List<HealthCommand> healthCommands;

    @Autowired
    HealthController(ConfigurationHealthCommand infoCommand, List<HealthCommand> healthCommands) {
        this.infoCommand = infoCommand;
        this.healthCommands = healthCommands;
    }

    @GetMapping
    public JsonNode config() {
        return infoCommand.execute();
    }

    @GetMapping("detail")
    public JsonNode detail() {
        JsonObjects.Builder builder = JsonObjects.builder();
        for (HealthCommand command : healthCommands) {
            builder.attr(command.name(), command.measuredExecution());
        }
        return builder.build();
    }
}
