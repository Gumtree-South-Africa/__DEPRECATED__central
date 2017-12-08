package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.persistence.MessageNotFoundException;

public class RiakHeldMailRepository implements HeldMailRepository {
    private MailRepository mailRepository;

    public RiakHeldMailRepository(MailRepository mailRepository) {
        this.mailRepository = mailRepository;
    }

    @Override
    public byte[] read(String messageId) throws MessageNotFoundException {
        byte[] content = mailRepository.readInboundMail(messageId);

        if (content == null) {
            throw new MessageNotFoundException("Could not load held mail data by message id '" + messageId + "'");
        }

        return content;
    }

    @Override
    public void write(String messageId, byte[] content) {
        // Do nothing - writing already happens on the mailRepository
    }

    @Override
    public void remove(String messageId) {
        // Do nothing - removing already happens on the mailRepository
    }
}