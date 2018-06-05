package com.ecg.messagebox.util.messages;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class KjcaMessageResponseFactoryTest extends MessageResponseFactoryTestBase {

    private final KjcaMessageResponseFactory factory = new KjcaMessageResponseFactory();

    @Test
    public void testParsingTemplate() throws IOException {
        readMessageFromFile("/kjca-templated-email.txt");

        String message = factory.getCleanedMessage(null, this.message);

        assertThat(message).isEqualTo("Hi, I'm interested! Please contact me if this is still available. Test.");
    }
}
