/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ecg.replyts.core.api.model.mail;

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

}
