package com.ecg.replyts.core.api.model.mail;

import com.google.common.net.MediaType;

public abstract class TypedContent<T> {

    private MediaType mediaType;
    private T content;

    public TypedContent() {
        //alternative constructor
    }

    public TypedContent(MediaType mediaType, T content) {
        this.mediaType = mediaType;
        this.content = content;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public T getContent() {
        return content;
    }

    protected void setContent(T content) throws IllegalStateException {
        if (!isMutable()) {
            throw new IllegalStateException();
        }
        this.content = content;
    }

    /**
     * tells if this mail body can be changed
     *
     * @return
     */
    public abstract boolean isMutable();

    /**
     * sets a new content for this part. this operation is only allowed when this part is
     * {@link TypedContent#isMutable()}. If this mail is not mutable, this method will throw an
     * {@link IllegalStateException}.
     *
     * @param newContent
     * @throws IllegalStateException if mail is not mutable
     */
    public abstract void overrideContent(T newContent) throws IllegalStateException;
}
