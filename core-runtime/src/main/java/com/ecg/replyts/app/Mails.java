package com.ecg.replyts.app;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.mailparser.StructuredMail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Contains functionality to read mails from input streams and write mails to an output stream or a byte array
 */
public class Mails {

    private static final Timer PARSE_TIMER = TimingReports.newTimer("mail-parse");
    private static final Timer GENERATE_TIMER = TimingReports.newTimer("mail-generate");

    /**
     * Attemts to parse a mail. If not possible, throws an exception
     */
    public Mail readMail(byte[] input) throws ParsingException {
        Timer.Context timer = PARSE_TIMER.time();
        try {
            return StructuredMail.parseMail(new ByteArrayInputStream(input));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            timer.stop();
        }
    }

    /**
     * writes the given mail object into a byte array and returns it.
     */
    public byte[] writeToBuffer(Mail mail) {
        Timer.Context timer = GENERATE_TIMER.time();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            mail.writeTo(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            timer.stop();
        }
    }
}
