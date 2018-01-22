package com.ecg.replyts.app.mailreceiver.kafka;

import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.google.common.base.Strings;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Random;

public class ShadowTestingFramework9000 {
    public static void maybeDoAThing() throws ParsingException, IOException {
        // One message in each n should throw a ParsingException (random distribution)
        int chanceOfParsingException = getEnv("KAFKA_CHANCE_OF_PARSING_EXCEPTION");
        if (chanceOfParsingException > 0 && 0 == new Random().nextInt(chanceOfParsingException)) {
            throw new ParsingException("A random parsing exception");
        }

        // Throw IOExceptions every first n minutes of the hour, exclusive
        int throwIOExceptionsInMinutes = getEnv("KAFKA_THROW_IO_EXCEPTIONS_IN_MINUTES");
        if (throwIOExceptionsInMinutes > 0 && throwIOExceptionsInMinutes > LocalTime.now().getMinute()) {
            throw new IOException("A random IO exception");
        }
    }

    private static Integer getEnv(String var) {
        String raw = System.getenv(var);
        if (Strings.isNullOrEmpty(raw)) {
            return -1;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignore) {
            return -1;
        }
    }
}
