package com.ecg.messagebox.model;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableAttachment.class)
@JsonDeserialize(as = ImmutableAttachment.class)
public abstract class Attachment {
    public abstract String fileName();

    public abstract String messageID();
}
