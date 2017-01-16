package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.persistence.MailRepository;

public class RiakHeldMailRepository implements HeldMailRepository {
    private MailRepository mailRepository;

    public RiakHeldMailRepository(MailRepository mailRepository) {
        this.mailRepository = mailRepository;
    }

    @Override
    public byte[] read(String messageId) {
        byte[] content = mailRepository.readInboundMail(messageId);

        if (content == null) {
            throw new RuntimeException("Could not load held mail content by message id #" + messageId);
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