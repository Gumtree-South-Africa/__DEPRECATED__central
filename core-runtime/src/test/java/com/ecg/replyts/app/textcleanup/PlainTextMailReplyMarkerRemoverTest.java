package com.ecg.replyts.app.textcleanup;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PlainTextMailReplyMarkerRemoverTest {

    @Test
    public void testEnsureGreaterThanSymbolIsRemoved() throws Exception {

        String testEmail = new String(">test email starting with a greater than symbol\n" +
                ">second line");

        PlainTextMailReplyMarkerRemover plainTextMailReplyMarkerRemover = new PlainTextMailReplyMarkerRemover();
        testEmail = plainTextMailReplyMarkerRemover.remove(testEmail);

        assertThat(testEmail.contains(">"), equalTo(false));
    }

    @Test
    public void testLeaveGreaterThanSymbolIfStartLine() throws Exception {
        String originalMail = "two > one";
        String testMail = new PlainTextMailReplyMarkerRemover().remove(originalMail);

        assertThat(testMail, equalTo(originalMail));
    }

    @Test
    public void testRemoveAllLeadingGreaterThanSymbolsAndLeaveInlineSymbols() throws Exception {
        String originalMail = ">>>>two > one";
        String testMail = new PlainTextMailReplyMarkerRemover().remove(originalMail);

        assertThat(testMail, equalTo("two > one"));
    }
}
