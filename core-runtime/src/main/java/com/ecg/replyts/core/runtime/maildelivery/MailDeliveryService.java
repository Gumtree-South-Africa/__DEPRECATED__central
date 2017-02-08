package com.ecg.replyts.core.runtime.maildelivery;

import com.ecg.replyts.core.api.model.mail.Mail;

public interface MailDeliveryService {
    void deliverMail(Mail m) throws MailDeliveryException;
}
