package com.ecg.messagecenter.webapi.responses;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AttachmentTest {

    @Test
    public void escapesFilenamesCorrect() {
        Assert.assertEquals("/screeningv2/mail/123/INBOUND/parts/foo%20bar.jpg", new MessageResponse.Attachment("foo bar.jpg", "123").getUrl());
    }

    @Test
    public void detectsMimeTypeCorrectly() {
        Assert.assertEquals("image/jpeg", new MessageResponse.Attachment("dsf.jpg", "12").getFormat());

    }

    @Test
    public void defaultsToApplicationBinary() {
        Assert.assertEquals("application/binary", new MessageResponse.Attachment("adf.asdfasdfadsf", "s").getFormat());
    }

    @Test
    public void defaultsToDefaultIfNoExtensionPresent() {
        Assert.assertEquals("application/binary", new MessageResponse.Attachment("adf", "s").getFormat());
    }
}
