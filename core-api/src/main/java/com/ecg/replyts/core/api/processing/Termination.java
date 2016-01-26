package com.ecg.replyts.core.api.processing;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.google.common.base.Objects;

import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * Describes why mail processing was stopped. A termination does not refer to an abnormal end of message processing - it just
 * means that ReplyTS is finished with processing that message for now and gives details on the reason.
 */
public class Termination {
    private final String reason;
    private final Class<?> issuer;
    private final MessageState endState;

    public String getReason() {
        return reason;
    }

    public Class getIssuer() {
        return issuer;
    }

    public MessageState getEndState() {
        return endState;
    }

    public Termination(MessageState endState, Class<?> issuer, String reason) {
        this.endState = endState;
        this.issuer = issuer;
        this.reason = reason;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Termination that = (Termination) o;

        return Objects.equal(endState, that.getEndState()) && Objects.equal(issuer, that.issuer) && Objects.equal(reason, that.reason);
    }

    public int hashCode() {
        return Objects.hashCode(endState, issuer, reason);
    }

    public static Termination unparseable(Exception e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return new Termination(MessageState.UNPARSABLE, Mail.class, stringWriter.toString());
    }

    public static Termination sent() {
        return new Termination(MessageState.SENT, Message.class, "sent");
    }

}
