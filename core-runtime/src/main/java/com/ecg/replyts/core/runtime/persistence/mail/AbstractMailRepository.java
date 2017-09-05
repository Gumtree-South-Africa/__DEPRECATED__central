package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;

import java.util.Optional;

import static com.ecg.replyts.core.runtime.persistence.mail.StoredMail.extract;

/**
 * Created by pragone
 * Created on 15/10/15 at 10:22 PM
 *
 * @author Paolo Ragone <pragone@ebay.com>
 */
public abstract class AbstractMailRepository implements MailRepository {

    @Override
    public void persistMail(String messageId, byte[] mailData, Optional<byte[]> outgoingMailData) {
        this.doPersist(messageId, new StoredMail(mailData, outgoingMailData).compress());
    }

    protected abstract void doPersist(String messageId, byte[] compress);

    protected abstract Optional<byte[]> doLoad(String messageId);

    @Override
    public byte[] readInboundMail(String messageId) {
        return this.doLoad(messageId)
                .map(mailData -> extract(mailData).getInboundContents())
                .orElse(null);
    }

    @Override
    public byte[] readOutboundMail(String messageId) {

        return this.doLoad(messageId)
                .map(mailData -> extract(mailData).getOutboundContents().orElse(null))
                .orElse(null);
    }

    @Override
    public Mail readInboundMailParsed(String messageId) {
        try {
            return new Mails().readMail(readInboundMail(messageId));
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Mail readOutboundMailParsed(String messageId) {
        try {
            return new Mails().readMail(readOutboundMail(messageId));
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }
    }

}
