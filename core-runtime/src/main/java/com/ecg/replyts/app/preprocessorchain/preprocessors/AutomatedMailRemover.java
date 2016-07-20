package com.ecg.replyts.app.preprocessorchain.preprocessors;


import com.ecg.replyts.app.preprocessorchain.PreProcessor;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.net.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * PreProcessor, that will put in some effort to check if the mail received can be classified as a bounce mail
 * (automatic reply to another mail). Such a mail could be:
 * <ol>
 * <li>An automatic sent Out of office message that is sent back by the receiver of a previously sent mail</li>
 * <li>A delivery status notification, like: "mailbox full", or "receiver does not exist"</li>
 * <li>Any mail like a newsletter, that classifies itself as Bulk by the <code>Precedence: bulk</code> or
 * <code>Precedence: junk</code> as "unimportant"</li>
 * </ol>
 * <p/>
 * <h3>Prerequisites</h3> This {@link com.ecg.replyts.app.preprocessorchain.PreProcessor} does not need any processing of a raw mail in advance. <h3>
 * Output</h3> This PreProcessor will not attach any extra data to the message. However, if it detects that a mail is a
 * bounce mail, it will block it and set it's state to {@link MessageState#IGNORED}
 *
 * @author huttar
 */
@Component("automatedMailRemover")
public class AutomatedMailRemover implements PreProcessor {


    private static final Logger LOG = LoggerFactory.getLogger(AutomatedMailRemover.class);

    // NOTE: "auto_reply" is NOT ignored
    private static final Set<String> IGNORABLE_PRECEDENCES = new HashSet<String>(Arrays.asList(
            "bulk", "junk", "list"));
    private static final String X_MAIL_AUTOREPLY = "X-Mail-Autoreply";
    private static final String X_AUTOREPLY = "X-Autoreply";
    private static final String X_LOOP = "X-Loop";
    private static final String RETURN_PATH = "Return-Path";
    private static final String NULL_VALUE = "<>";
    private static final String MAILER_DAEMON_VALUE = "<MAILER-DAEMON>";

    private static final String AUTO_SUBMITTED = "Auto-Submitted";
    private static final MediaType REPORT_MEDIATYPE = MediaType.parse("Multipart/report");
    private static final String MAILER_DAEMON = "MAILER-DAEMON@";
    private static final String X_AUTO_RESPONSE_SUPPRESS = "X-Auto-Response-Suppress";

    @Override
    public void preProcess(MessageProcessingContext context) {
        Mail mail = context.getMail();


        boolean isAcceptableMail = checkReturnPath(mail, context) && // NOSONAR
                checkFromMailerDaemon(mail, context) &&
                checkAutoSubmitted(mail, context) &&
                checkXLoop(mail, context) &&
                checkAutoReply(mail, context) &&
                checkPrecedence(mail, context) &&
                checkContentType(mail, context) &&
                checkAutoResponse(mail, context);
        if (!isAcceptableMail) {
            LOG.debug("mail is automated reply");
        }

    }

    private boolean checkReturnPath(final Mail mail, final MessageProcessingContext ctx) {
        String returnPathHeader = mail.getUniqueHeader(RETURN_PATH);
        if (NULL_VALUE.equalsIgnoreCase(returnPathHeader) || MAILER_DAEMON_VALUE.equalsIgnoreCase(returnPathHeader)) {
            ctx.terminateProcessing(MessageState.IGNORED, this, "Is auto generated mail (empty return path)");
            return false;
        }
        return true;
    }

    private boolean checkAutoSubmitted(final Mail mail, final MessageProcessingContext ctx) {
        String h = mail.getUniqueHeader(AUTO_SUBMITTED);
        if (h != null && !h.equalsIgnoreCase("no")) {
            ctx.terminateProcessing(MessageState.IGNORED, this, "Is auto generated mail (Auto-Submitted Header value is not no)");
            return false;
        }
        return true;
    }

    private boolean checkContentType(final Mail mail, final MessageProcessingContext ctx) {
        MediaType contentType = mail.getMainContentType();
        if (contentType != null && REPORT_MEDIATYPE.is(contentType.withoutParameters())) {
            ctx.terminateProcessing(MessageState.IGNORED, this, "Is delivery status notification (Content-Type " + contentType + ")");
            return false;
        }
        return true;
    }

    private boolean checkFromMailerDaemon(final Mail mail, final MessageProcessingContext ctx) {
        String from = mail.getFrom();
        if (from != null && from.toUpperCase().contains(MAILER_DAEMON)) {
            ctx.terminateProcessing(MessageState.IGNORED, this, "Is from mailer daemon (sender contains MAILERDAEMON)");
            return false;
        }
        return true;
    }

    private boolean checkXLoop(final Mail mail, final MessageProcessingContext ctx) {
        final String from = mail.getFrom();
        final String xloop = mail.getUniqueHeader(X_LOOP);
        if (from != null && xloop != null && from.equalsIgnoreCase(xloop)) {
            ctx.terminateProcessing(MessageState.IGNORED, this, "Is auto generated mail (X-Loop equals From header)");
            return false;
        }
        return true;
    }

    private boolean checkAutoReply(final Mail mail, final MessageProcessingContext ctx) {
        if (mail.containsHeader(X_AUTOREPLY) || mail.containsHeader(X_MAIL_AUTOREPLY)) {
            ctx.terminateProcessing(MessageState.IGNORED, this, "Is auto reply (has X-Autoreply or X-Mail-Autoreply Header)");
            return false;
        }
        return true;
    }

    private boolean checkPrecedence(final Mail mail, final MessageProcessingContext ctx) {
        return checkPrecedence(mail, "Precedence", ctx) && checkPrecedence(mail, "X-Precedence", ctx);
    }

    private boolean checkPrecedence(Mail mail, String precedenceHeader, final MessageProcessingContext ctx) {
        final String precedence = mail.getUniqueHeader(precedenceHeader);
        if (precedence != null && IGNORABLE_PRECEDENCES.contains(precedence.toLowerCase())) {
            ctx.terminateProcessing(MessageState.IGNORED, this, "Is Bounce Mail (has " + precedenceHeader + ": " + precedence + ")");
            return false;
        }
        return true;
    }

    private boolean checkAutoResponse(Mail mail, MessageProcessingContext context) {
        // https://msdn.microsoft.com/en-us/library/ee219609%28v=exchg.80%29.aspx
        String h = mail.getUniqueHeader(X_AUTO_RESPONSE_SUPPRESS);
        if (h != null) {
            context.terminateProcessing(MessageState.IGNORED, this, "Is auto response (" + X_AUTO_RESPONSE_SUPPRESS + " has value '" + h + "')");
            return false;
        }
        return true;
    }

}
