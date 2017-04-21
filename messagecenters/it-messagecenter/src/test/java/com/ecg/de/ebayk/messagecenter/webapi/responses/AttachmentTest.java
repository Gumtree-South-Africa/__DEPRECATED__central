package com.ecg.de.ebayk.messagecenter.webapi.responses;

import org.junit.Test;

import static com.ecg.de.ebayk.messagecenter.webapi.responses.MessageResponse.Attachment;
import static org.junit.Assert.assertEquals;

public class AttachmentTest {

    @Test public void escapesFilenamesCorrect() {
        assertEquals("/screeningv2/mail/123/INBOUND/parts/foo%20bar.jpg",
                        new Attachment("foo bar.jpg", "123").getUrl());
    }

    @Test public void detectsMimeTypeCorrectly() {
        assertEquals("image/jpeg", new Attachment("dsf.jpg", "12").getFormat());

    }

    @Test public void defaultsToApplicationBinary() {
        assertEquals("application/binary", new Attachment("adf.asdfasdfadsf", "s").getFormat());
    }

    @Test public void defaultsToDefaultIfNoExtensionPresent() {
        assertEquals("application/binary", new Attachment("adf", "s").getFormat());
    }
}
