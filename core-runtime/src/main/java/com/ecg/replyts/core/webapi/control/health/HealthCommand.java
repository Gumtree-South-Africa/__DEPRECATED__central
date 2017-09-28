package com.ecg.replyts.core.webapi.control.health;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;

/**
 * Implementation checks his specific way an external service and create a response in {@link ObjectNode}.
 */
public interface HealthCommand {

    /**
     * Executes a current command and returns {@link ObjectNode} which corresponds to the result of current command. Json as a response
     * should contain information about the connections to external services and also convenient detailed information about the service
     * to make the following investigation easier.
     *
     * @return Json with basic and detailed information about external service.
     */
    ObjectNode execute();

    /**
     * Method returns name of the current command. Can be also used as a key in final Json result.
     *
     * @return name of hte command.
     */
    String name();

    /**
     * Return the same {@link ObjectNode} as {@link #execute()} function but adds duration field of the execution in millis.
     *
     * @return executed command with time duration in millis.
     */
    default ObjectNode measuredExecution() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ObjectNode result;
        try {
            result = execute();
        } catch (Exception ex) {
            result = JsonObjects.newJsonObject()
                    .put("error", ex.getMessage());
        }
        stopwatch.stop();
        return result.put("duration", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
}
