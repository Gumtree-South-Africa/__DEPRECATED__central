/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ecg.replyts.core.api.model.mail;

import com.ecg.replyts.core.api.processing.MessageFixer;

import java.util.List;

/**
 * @author alindhorst
 */
public interface MutableMail extends Mail {

    /**
     * Sets or <i>replaces</i> a header. (WARNING: So it does not actually <i>add</i> if its already there.)
     */
    void addHeader(String name, String value);

    /**
     * Removes all headers with the given name (case insensitive).
     */
    void removeHeader(String name);

    /**
     * Change the sender of the mail.
     * Mail must be mutable.
     *
     * @param newFrom new sender (not null)
     */
    void setFrom(MailAddress newFrom);

    /**
     * Change the recipient of the mail.
     * Mail must be mutable.
     *
     * @param newTo new recipient (not null)
     */
    void setTo(MailAddress newTo);

    /**
     * Change the reply to of the mail.
     * Mail must be mutable.
     *
     * @param newReplyTo new reply to (not null)
     */
    void setReplyTo(MailAddress newReplyTo);

    /**
     * Try to apply some fixes to this message in order to send it successfully.
     * Some clients send email that isn't valid, but isn't so broken that it can't be fixed.
     * This method should be called explicitly when a sending error occurs, since the vast
     * majority of email is valid and doesn't need any special handling.
     *
     * @param fixers            List of available message-fixers.
     * @param originalException Exception that triggered the mail fixing process.
     */
    void applyOutgoingMailFixes(List<MessageFixer> fixers, Exception originalException);
}
