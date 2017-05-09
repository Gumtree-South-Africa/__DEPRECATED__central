package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.google.common.net.MediaType;

public class AttachmentTypedContent extends TypedContent {

    public AttachmentTypedContent(MediaType type, byte[] data) {
        super(type, data);
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public void overrideContent(Object newContent) throws IllegalStateException {
        throw new UnsupportedOperationException();
    }
}
