package com.ecg.replyts.core.runtime;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import net.logstash.logback.composite.loggingevent.ArgumentsJsonProvider;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.*;

@Component
public class LoggingService {
    private static final LoggerContext LOGGER_CONTEXT = (LoggerContext) LoggerFactory.getILoggerFactory();

    private static final String LOG_LEVEL_PREFIX = "log.level.";

    @Autowired
    private ConfigurableEnvironment environment;

    @Value("#{T(ch.qos.logback.classic.Level).toLevel('${log.level.ROOT:INFO}')}")
    private Level rootLevel;

    private Map<Logger, Level> levels = new ConcurrentHashMap<>();

    public void initialize() {
        LOGGER_CONTEXT.putProperty(TENANT, environment.getProperty("replyts.tenant", "unknown"));

        initializeToProperties();
    }

    public Map<String, String> getLevels() {
        return levels.keySet().stream()
          .collect(Collectors.toMap(logger -> logger.getName(), logger -> levels.get(logger).toString()));
    }

    public void replaceAll(Map<String, String> newLevels) {
        Logger actualRootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        if (newLevels.containsKey(Logger.ROOT_LOGGER_NAME)) {
            this.rootLevel = Level.valueOf(newLevels.get(Logger.ROOT_LOGGER_NAME));
        } else {
            this.rootLevel = Level.INFO;
            actualRootLogger.setLevel(rootLevel);
        }

        synchronized (levels) {
            levels.keySet().stream()
              .filter(logger -> !logger.equals(actualRootLogger))
              .forEach(logger -> logger.setLevel(null));

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
          .forEach(logger -> upsertAndSet(logger, environment.getProperty(LOG_LEVEL_PREFIX + logger)));

        Logger actualRootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        if (levels.containsKey(actualRootLogger)) {
            this.rootLevel = levels.get(actualRootLogger);
        } else {
            this.rootLevel = Level.INFO;
            actualRootLogger.setLevel(rootLevel);
        }

        oldLevels.keySet().stream()
          .filter(logger -> !levels.containsKey(logger))
          .forEach(logger -> logger.setLevel(null));
    }

    public static void bootstrap() {
        Thread.currentThread().setName("main");

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        rootLogger.setLevel(Level.INFO);

        // Make it possible to disable structured logging for the purpose of running locally

        if (Boolean.parseBoolean(System.getProperty("logging.service.structured.logging", "true"))) {
            ConsoleAppender<ILoggingEvent> appender = StreamSupport.stream(Spliterators.spliteratorUnknownSize(rootLogger.iteratorForAppenders(), Spliterator.ORDERED), false)
              .filter(a -> a instanceof ConsoleAppender<?>)
              .map(a -> (ConsoleAppender<ILoggingEvent>) a)
              .findFirst().orElseThrow(() -> new RuntimeException("Root logger doesn't contain a console appender"));

            appender.setTarget(ConsoleTarget.SystemErr.getName());
            appender.setOutputStream(ConsoleTarget.SystemErr.getStream());

            try {
                LogstashEncoder encoder = new LogstashEncoder();

                encoder.getFieldNames().setLevelValue("[ignore]");
                encoder.getProviders().addProvider(new ArgumentsJsonProvider());

                encoder.init(appender.getOutputStream());
                encoder.start();

                appender.setEncoder(encoder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        LOGGER_CONTEXT.putProperty(APPLICATION, ReplyTS.class.getPackage().getImplementationTitle());
        LOGGER_CONTEXT.putProperty(REVISION, ReplyTS.class.getPackage().getImplementationVersion());
    }
}
