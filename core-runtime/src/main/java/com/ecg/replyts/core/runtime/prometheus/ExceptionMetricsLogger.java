package com.ecg.replyts.core.runtime.prometheus;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.prometheus.client.Counter;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExceptionMetricsLogger {
    public ExceptionMetricsLogger() {
        LoggerContext factory = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = factory.getLogger(ExceptionMetricsLogger.class);
        logger.info("Setting up Prometheus logback metrics plugin");
        Logger root = factory.getLogger(Logger.ROOT_LOGGER_NAME);
        InstrumentedAppender instrumentedAppender = new InstrumentedAppender();
        instrumentedAppender.setContext(root.getLoggerContext());
        instrumentedAppender.start();
        root.addAppender(instrumentedAppender);
    }

    private class InstrumentedAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

        private Counter logsEmitted;

        @Override
        public void start() {
            logsEmitted = Counter.build("log_lines_emitted", "Number of log lines emitted").labelNames("level")
                    .register();
            super.start();
        }

        @Override
        protected void append(ILoggingEvent event) {
            switch (event.getLevel().toInt()) {
            case Level.WARN_INT:
                logsEmitted.labels("warn").inc();
                break;
            case Level.ERROR_INT:
                logsEmitted.labels("error").inc();
                break;
            default:
                break;
            }
        }
    }

}
