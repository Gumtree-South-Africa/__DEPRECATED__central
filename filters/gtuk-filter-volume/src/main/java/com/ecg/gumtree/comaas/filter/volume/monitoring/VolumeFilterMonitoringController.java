package com.ecg.gumtree.comaas.filter.volume.monitoring;

import com.ecg.gumtree.comaas.filter.volume.EventStreamProcessor;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gumtree.filters.comaas.config.VelocityFilterConfig;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * HTTP Controller which is able to expose current state of windows in volume-filters. This controller should not be exposed in Production
 * therefore it's possible to enable/disable it using the property {@code volume-filter.monitoring.enabled}.
 */
@RestController
public class VolumeFilterMonitoringController {

    private EventStreamProcessor eventStreamProcessor;

    VolumeFilterMonitoringController(EventStreamProcessor eventStreamProcessor) {
        this.eventStreamProcessor = eventStreamProcessor;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ObjectNode getSummary() {
        EventStreamProcessor.Summary summary = eventStreamProcessor.getSummary();

        Map<String, VelocityFilterConfig> configs = summary.getVelocityFilterConfigs();

        JsonObjects.Builder configsJson = JsonObjects.builder();
        for (Map.Entry<String, VelocityFilterConfig> entry : configs.entrySet()) {
            VelocityFilterConfig config = entry.getValue();
            JsonObjects.Builder configJson = JsonObjects.builder()
                    .attr("messages", config.getMessages())
                    .attr("seconds", config.getSeconds())
                    .attr("whitelistSeconds", config.getWhitelistSeconds())
                    .attr("exceeding", config.isExceeding())
                    .attr("priority", config.getPriority())
                    .attr("version", config.getVersion())
                    .attr("result", config.getResult().name())
                    .attr("state", config.getState().name())
                    .attr("exemptedCategories", config.getExemptedCategories().toString())
                    .attr("filterField", config.getFilterField().name());
            configsJson.attr(entry.getKey(), configJson.build());
        }

        return JsonObjects.builder()
                .attr("eventTypes", summary.getEvents())
                .attr("windows", summary.getInternalWindows())
                .attr("configs", configsJson)
                .build();
    }

    @RequestMapping(value = "/{field:.+}", method = RequestMethod.GET)
    public ObjectNode getCount(@PathVariable("field") String field) {
        EventStreamProcessor.Summary summary = eventStreamProcessor.getSummary();

        JsonObjects.Builder configJson = JsonObjects.builder();
        for (String window : summary.getInternalWindows()) {
            configJson.attr(window, eventStreamProcessor.count(field, window.replaceFirst("volume", "")));
        }

        return configJson.build();
    }
}
