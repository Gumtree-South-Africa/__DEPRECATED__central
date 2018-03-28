package com.ecg.messagebox.util.messages;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class GtauMessageResponseFactoryTest extends MessageResponseFactoryTestBase {

    private final GtauMessageResponseFactory factory = new GtauMessageResponseFactory();

    @Test
    public void whenNoMessage_shouldReturnEmptyString() throws IOException {
        when(message.getPlainTextBody()).thenReturn("");

        String message = factory.getCleanedMessage(null, this.message);

        assertThat(message).isEmpty();
    }

    @Test
    public void whenCleanMessage_shouldReturnItself() throws IOException {
        when(message.getPlainTextBody()).thenReturn("Hello from test");

        String message = factory.getCleanedMessage(null, this.message);

        assertThat(message).isEqualTo("Hello from test");
    }

    @Test
    public void whenDirtyMessage_shouldReturnClean() throws IOException {
        readMessageFromFile("/gtau-templated-email.txt");

        String message = factory.getCleanedMessage(null, this.message);

        assertThat(message).isEqualTo("Hi Oleksandr,\n" +
                "I'm interested in \"Comaas Ad\". Is this still available? If so, when and where can I pick it up?\n" +
                "Cheers");
    }
}
