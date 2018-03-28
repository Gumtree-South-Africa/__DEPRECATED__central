package com.ecg.messagebox.util.messages;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class GtukMessageResponseFactoryTest extends MessageResponseFactoryTestBase {

    private final GtukMessageResponseFactory factory = new GtukMessageResponseFactory();

    @Test
    public void testParsingTemplate() throws IOException {
        readMessageFromFile("/gtuk-templated-email.txt");

        String message = factory.getCleanedMessage(null, this.message);

        assertThat(message).isEqualTo("test 1st message");
    }
}
