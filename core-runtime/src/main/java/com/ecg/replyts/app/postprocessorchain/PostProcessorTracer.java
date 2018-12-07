package com.ecg.replyts.app.postprocessorchain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A global thingy to trace what parts of the PostProcessor magic is running. Advantage is you can grep for the classname
 * to see all dispersed components' logging now.
 */
public class PostProcessorTracer {
    private static final Logger LOG = LoggerFactory.getLogger(PostProcessorTracer.class);

    // concurrent set
    private static Map<PostProcessor, Boolean> alreadyCalledApplicableProcessors = new ConcurrentHashMap();

    private static Map<PostProcessor, Boolean> alreadyCalledNotApplicableProcessors = new ConcurrentHashMap();

    public static void logInit(PostProcessor p) {

    }

    public static void logStart(PostProcessor p) {

    }

    public static void info(Object component, String fmt, Object... args) {
        LOG.info("component=" + component.getClass().getName() + ": " + fmt, args);
    }
    public static void debug(Object component, String fmt, Object... args) {
        LOG.debug("component=" + component.getClass().getName() + ": " + fmt, args);
    }

    public static void logPostProcessorApplicable(PostProcessor p) {
        alreadyCalledApplicableProcessors.computeIfAbsent(p, postProcessorInstance -> {
            info(postProcessorInstance, "applicable (Logging only once)");
            return true;
        });
    }
    public static void logPostProcessorNotApplicable(PostProcessor p) {
        alreadyCalledNotApplicableProcessors.computeIfAbsent(p, postProcessorInstance -> {
            info(postProcessorInstance, "NOT applicable (Logging only once)");
            return true;
        });
    }
}
