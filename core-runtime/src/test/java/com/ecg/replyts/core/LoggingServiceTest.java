package com.ecg.replyts.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = LoggingServiceTest.TestContext.class)
@TestPropertySource(properties = {
  "replyts.tenant = foo",

  "log.level.ROOT = ERROR",
  "log.level.foo.bar = DEBUG"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class LoggingServiceTest {
    private static final LoggerContext LOGGER_CONTEXT = (LoggerContext) LoggerFactory.getILoggerFactory();

    @Autowired
    private LoggingService loggingService;

    @Before
    public void initialize() {
        loggingService.initialize();
    }
  
    @Test
    public void testContext() {
        assertEquals("Logger context contains tenant", "foo", LOGGER_CONTEXT.getProperty("tenant"));
    }

    @Test
    public void testRootLevel() {
        Level rootLevel = (Level) ReflectionTestUtils.getField(loggingService, "rootLevel");

        assertEquals("Root level is set to ERROR after initialization as per the TestPropertySource", Level.ERROR, rootLevel);
        assertEquals("Actual Logback ROOT level was set to ERROR", Level.ERROR, ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).getLevel());
    }

    @Test
    public void testLevelOverridesAfterInit() {
        assertTrue("Initial set of logging statements contains 'foo.bar'", loggingService.getLevels().containsKey("foo.bar"));
        assertEquals("Initial set of logging statements contains 'foo.bar' set to DEBUG", "DEBUG", loggingService.getLevels().get("foo.bar"));
    }

    @Test
    public void testUpsertAndSet() {
        assertFalse("Initial set of logging statements does not contain 'baz.foz'", loggingService.getLevels().containsKey("baz.foz"));

        loggingService.upsertAndSet("baz.foz", "DEBUG");

        assertTrue("Updated set of logging statements does contain 'baz.foz'", loggingService.getLevels().containsKey("baz.foz"));
        assertEquals("Updated set of logging statements contains 'baz.foz' set to DEBUG", "DEBUG", loggingService.getLevels().get("baz.foz"));

        assertEquals("Actual Logback level for 'baz.foz' was set to DEBUG", Level.DEBUG, ((Logger) LoggerFactory.getLogger("baz.foz")).getLevel());
    }

    @Test
    public void testUpsertAndSetAndThenResetToInitialProperties() {
        loggingService.upsertAndSet("baz.foz", "DEBUG");

        loggingService.initializeToProperties();

        assertFalse("Reset-to-original set of logging statements does not contain 'baz.foz'", loggingService.getLevels().containsKey("baz.foz"));
    }

    @Test
    public void testReplaceAll() {
        assertTrue("Initial set of logging statements contains 'foo.bar'", loggingService.getLevels().containsKey("foo.bar"));
        assertEquals("Initial set of logging statements contains 'foo.bar' set to DEBUG", "DEBUG", loggingService.getLevels().get("foo.bar"));

        loggingService.replaceAll(new HashMap<String, String>() {{
            put("baz.foz", "DEBUG");
        }});

        assertTrue("Replaced set of logging statements does contain 'baz.foz'", loggingService.getLevels().containsKey("baz.foz"));
        assertEquals("Replaced set of logging statements contains 'baz.foz' set to DEBUG", "DEBUG", loggingService.getLevels().get("baz.foz"));
        assertFalse("Replaced set of logging statements does not contain 'foo.bar'", loggingService.getLevels().containsKey("foo.bar"));
    }

    @Test
    public void testClassOverride() {
        String className = getClass().getTypeName();

        loggingService.replaceAll(new HashMap<String, String>() {{
            put(Logger.ROOT_LOGGER_NAME, "ERROR");
            put(getClass().getPackage().getName(), "WARN");
            put(className, "INFO");
        }});

        assertEquals("Actual Logback level for package was set to WARN", Level.WARN, ((Logger) LoggerFactory.getLogger(getClass().getPackage().getName())).getLevel());
        assertEquals("Actual Logback level for class was set to INFO", Level.INFO, ((Logger) LoggerFactory.getLogger(className)).getLevel());

        assertTrue("Actual Logback indicates INFO is enabled for this class", LoggerFactory.getLogger(getClass()).isInfoEnabled());
        assertFalse("Actual Logback indicates DEBUG is disabled for this class", LoggerFactory.getLogger(getClass()).isDebugEnabled());
    }

    @Test
    public void testReplaceAllLogLevel() {
        loggingService.replaceAll(new HashMap<String, String>() {{
            put("ROOT", "DEBUG");
        }});

        Level rootLevel = (Level) ReflectionTestUtils.getField(loggingService, "rootLevel");

        assertEquals("Root level is set to DEBUG after initialization as per the TestPropertySource", Level.DEBUG, rootLevel);
    }

    @Configuration
    @Import(LoggingService.class)
    static class TestContext { }
}
