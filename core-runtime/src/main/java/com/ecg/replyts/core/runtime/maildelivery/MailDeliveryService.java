package com.ecg.replyts.core.runtime.maildelivery;

import com.ecg.replyts.core.api.model.mail.Mail;

/**
 * Defines the contract for a service that is in charge of delivering a mail without any further modification to it's
 * receiver.
 *
 * @author huttar
 */
public interface MailDeliveryService {
    /**
     * Delivers a Mail to the receiver(s) defined in the given mail
     *
     * @param m mail to deliver
     * @throws MailDeliveryException if the mail could not be delivered for any reasons. (These may be included as nested exceptions)
     */
    void deliverMail(Mail m) throws MailDeliveryException;

}
