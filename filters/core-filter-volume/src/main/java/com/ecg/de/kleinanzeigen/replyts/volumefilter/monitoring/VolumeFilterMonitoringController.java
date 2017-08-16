package com.ecg.de.kleinanzeigen.replyts.volumefilter.monitoring;

import com.ecg.de.kleinanzeigen.replyts.volumefilter.EventStreamProcessor;
import com.ecg.de.kleinanzeigen.replyts.volumefilter.Quota;
import com.ecg.de.kleinanzeigen.replyts.volumefilter.Window;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.nio.Address;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * HTTP Controller which is able to expose current state of windows in volume-filters. This controller should not be exposed in Production
 * therefore it's possible to enable/disable it using the property {@code volume-filter.monitoring.enabled}.
 */
@RestController
public class VolumeFilterMonitoringController {

    private static String NODE_STATUS_URL_PATTERN = "http://%s:%d/volume-filter/monitoring/{email}";

    private final HazelcastInstance hazelcastInstance;
    private final EventStreamProcessor eventStreamProcessor;
    private final int httpPort;
    private final RestTemplate restTemplate;

    VolumeFilterMonitoringController(HazelcastInstance hazelcastInstance, EventStreamProcessor eventStreamProcessor, int httpPort) {
        this.hazelcastInstance = hazelcastInstance;
        this.eventStreamProcessor = eventStreamProcessor;
        this.httpPort = httpPort;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Returns summary of all registered volume-filters their configuration and all registered windows in Esper windowing system.
     *
     * @return returns summary configuration of current volume filters.
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ObjectNode getSummary() {
        EventStreamProcessor.Summary summary = eventStreamProcessor.getSummary();

        JsonObjects.Builder pluginToQuotas = JsonObjects.builder();
        for (Map.Entry<String, Collection<Quota>> entry : summary.getInstanceIdToQuota().asMap().entrySet()) {
            ArrayNode quotaArray = JsonObjects.newJsonArray();
            for (Quota quota : entry.getValue()) {
                JsonObjects.Builder jsonQuota = JsonObjects.builder()
                        .attr("allowance", quota.getAllowance())
                        .attr("perTimeValue", quota.getPerTimeValue())
                        .attr("perTimeUnit", quota.getPerTimeUnit().toString())
                        .attr("score", quota.getScore());
                quotaArray.add(jsonQuota.build());
            }

            pluginToQuotas.attr(entry.getKey(), quotaArray);
        }

        return JsonObjects.builder()
                .attr("eventTypes", new ArrayList<>(summary.getEvents()))
                .attr("windows", summary.getInternalWindows())
                .attr("configurations", pluginToQuotas)
                .build();
    }

    /**
     * Returns detail information about the hits in all registered windows in Esper windowing system.
     *
     * @param email user email.
     * @return detailed information about the hits.
     */
    @RequestMapping(value = "/{email:.+}", method = RequestMethod.GET)
    public ObjectNode getCount(@PathVariable("email") String email) {
        EventStreamProcessor.Summary summary = eventStreamProcessor.getSummary();

        JsonObjects.Builder results = JsonObjects.builder();
        for (Window window : summary.getWindows()) {
            long count = eventStreamProcessor.count(email, window);
            results.attr(window.getWindowName(), count);
        }

        return results.build();
    }

    /**
     * Collects Esper's details across all nodes and returns current status of all windows of the particular {@code email}.
     *
     * @param email user's email.
     * @return current status of the Esper's across all nodes of the particular {@code email}.
     */
    @RequestMapping(value = "/{email:.+}/all", method = RequestMethod.GET)
    public ObjectNode getCountFromEntireCluster(@PathVariable("email") String email) {
        JsonObjects.Builder results = JsonObjects.builder();
        for (Member member : hazelcastInstance.getCluster().getMembers()) {
            Address address = member.getAddress();

            ResponseEntity<String> response =
                    restTemplate.getForEntity(String.format(NODE_STATUS_URL_PATTERN, address.getHost(), httpPort), String.class, email);

            if (response.getStatusCode() == HttpStatus.OK) {
                results.attr(address.getHost(), JsonObjects.parse(response.getBody()));
            } else {
                String error = String.format("Error occurred during count endpoint invocation. Status: %d", response.getStatusCodeValue());
                results.attr(address.getHost(), error);
            }
        }

        return results.build();
    }
}