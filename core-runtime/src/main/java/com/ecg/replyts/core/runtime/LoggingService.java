package com.ecg.replyts.core.runtime;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.stereotype.Component;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.*;

@Component
public class LoggingService {
    private static final LoggerContext LOGGER_CONTEXT = (LoggerContext) LoggerFactory.getILoggerFactory();

    private static final String LOG_LEVEL_PREFIX = "log.level.";

    static {
        Thread.currentThread().setName("main");

        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        LOGGER_CONTEXT.putProperty(APPLICATION, ReplyTS.class.getPackage().getImplementationTitle());
        LOGGER_CONTEXT.putProperty(REVISION, ReplyTS.class.getPackage().getImplementationVersion());
    }

    @Autowired
    private ConfigurableEnvironment environment;

    @Value("#{T(ch.qos.logback.classic.Level).toLevel('${log.level.ROOT:INFO}')}")
    private Level rootLevel;

    private Map<Logger, Level> levels = new ConcurrentHashMap<>();

    @PostConstruct
    private void initialize() {
        LOGGER_CONTEXT.putProperty(TENANT, environment.getProperty("replyts.tenant", "unknown"));

        initializeToProperties();
    }

    public Map<String, String> getLevels() {
        return levels.keySet().stream()
          .collect(Collectors.toMap(logger -> logger.getName(), logger -> levels.get(logger).toString()));
    }

    public void replaceAll(Map<String, String> newLevels) {
        if (newLevels.containsKey(Logger.ROOT_LOGGER_NAME)) {
            this.rootLevel = Level.valueOf(newLevels.get(Logger.ROOT_LOGGER_NAME));
        } else {
            this.rootLevel = Level.INFO;
        }

        synchronized (levels) {
            levels.forEach((logger, level) -> logger.setLevel(rootLevel));

            levels.clear();
        }

        newLevels.forEach((logPackage, logLevel) -> upsertAndSet(logPackage, logLevel));
    }

    public void upsertAndSet(String logPackage, String logLevel) {
        Logger logger = (Logger) LoggerFactory.getLogger(logPackage);
        Level level = Level.toLevel(logLevel);

        synchronized (levels) {
            levels.put(logger, level);
        }

        logger.setLevel(level);
    }

    public void initializeToProperties() {
        Map<Logger, Level> oldLevels;

        synchronized (levels) {
            oldLevels = new HashMap<>(levels);
            levels.clear();
        }

        StreamSupport.stream(environment.getPropertySources().spliterator(), false)
          .filter(source -> source instanceof EnumerablePropertySource)
          .flatMap(source -> Arrays.asList(((EnumerablePropertySource) source).getPropertyNames()).stream())
          .filter(property -> property.startsWith(LOG_LEVEL_PREFIX))
          .distinct()
          .map(property -> property.substring(LOG_LEVEL_PREFIX.length()).trim())
          .forEach(logger -> upsertAndSet(logger, environment.getProperty(logger)));

        if (levels.containsKey(Logger.ROOT_LOGGER_NAME)) {
            this.rootLevel = levels.get(Logger.ROOT_LOGGER_NAME);
        } else {
            this.rootLevel = Level.INFO;
        }

        oldLevels.keySet().stream()
          .filter(logger -> !levels.containsKey(logger))
          .forEach(logger -> logger.setLevel(rootLevel));
    }
}