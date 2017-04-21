package com.ecg.de.ebayk.messagecenter.webapi.requests;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by jaludden on 05/02/16.
 */
public class StartConversationContentPayloadTest {

    private StartConversationContentPayload payload;

    @Before public void setUp() {
        payload = new StartConversationContentPayload();
    }

    @Test public void testCleanAlreadyCleanedMessage() {
        payload.setMessage("Cleaned message");
        payload.cleanupMessage();
        assertThat(payload.getMessage(), is("Cleaned message"));
    }

    @Test public void testCleanOneLineMessage() {
        payload.setMessage(">Cleaned message");
        payload.cleanupMessage();
        assertThat(payload.getMessage(), is("Cleaned message"));
    }

    @Test public void testCleanFirstLine() {
        payload.setMessage(">Cleaned message\nAn other line\nAnd last line");
        payload.cleanupMessage();
        assertThat(payload.getMessage(), is("Cleaned message\n" +
                        "An other line\n" +
                        "And last line"));
    }

    @Test public void testCleanLastLine() {
        payload.setMessage("Cleaned message\n" +
                        "An other line\n" +
                        ">And last line");
        payload.cleanupMessage();
        assertThat(payload.getMessage(), is("Cleaned message\n" +
                        "An other line\n" +
                        "And last line"));
    }

    @Test public void testCleanMidLine() {
        payload.setMessage("Cleaned message\n" +
                        ">An other line\n" +
                        "And last line");
        payload.cleanupMessage();
        assertThat(payload.getMessage(), is("Cleaned message\n" +
                        "An other line\n" +
                        "And last line"));
    }

    @Test public void testCleanDoesNotCleanIfInsideText() {
        payload.setMessage("Cleaned> message\n" +
                        "An> other line\n" +
                        "And> last line");
        payload.cleanupMessage();
        assertThat(payload.getMessage(), is("Cleaned> message\n" +
                        "An> other line\n" +
                        "And> last line"));
    }

    @Test public void testCleanRemovesIfOnlyWhitespaceBefore() {
        payload.setMessage(" >Cleaned message\n" +
                        "\t>An other line\n" +
                        "\t >And last line");
        payload.cleanupMessage();
        assertThat(payload.getMessage(), is("Cleaned message\n" +
                        "An other line\n" +
                        "And last line"));
    }

    @Test public void testCleanMixedLine() {
        payload.setMessage(" >Cleaned >message\n" +
                        "\t>An >other >line\n" +
                        "\t >And >last >line");
        payload.cleanupMessage();
        assertThat(payload.getMessage(), is("Cleaned >message\n" +
                        "An >other >line\n" +
                        "And >last >line"));
    }

}
