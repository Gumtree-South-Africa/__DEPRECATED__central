package com.ecg.replyts.core.runtime.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.loggingevent.StackTraceJsonProvider;

import java.io.IOException;

public class EbaykStackTraceJsonProvider extends StackTraceJsonProvider {

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            generator.writeObjectFieldStart("exception");
            generator.writeStringField("stacktrace", getThrowableConverter().convert(event));
            generator.writeStringField("exception_class", throwableProxy.getClassName());
            generator.writeStringField("exception_message", throwableProxy.getMessage());
            generator.writeEndObject();
        }
    }
}
