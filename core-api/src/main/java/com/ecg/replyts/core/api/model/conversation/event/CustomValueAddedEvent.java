package com.ecg.replyts.core.api.model.conversation.event;

import com.ecg.replyts.core.api.util.Pairwise;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class CustomValueAddedEvent extends ConversationEvent {
    private final String key;
    private final String value;

    public CustomValueAddedEvent(@JsonProperty("key") String key, @JsonProperty("value") String value) {
        super("set-" + key.hashCode() + value.hashCode() + "-" + DateTime.now().getMillis(), DateTime.now());
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CustomValueAddedEvent that = (CustomValueAddedEvent) o;

        return Pairwise.pairsAreEqual(
                key, that.key,
                value, that.value,
                getConversationModifiedAt().getMillis(), that.getConversationModifiedAt().getMillis()
        );
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key, value, getConversationModifiedAt().getMillis());
    }
}
