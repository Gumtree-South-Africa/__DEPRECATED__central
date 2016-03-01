package com.ecg.replyts.app.eventpublisher;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageFilteredEvent;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import java.util.Collections;

public class EventTestUtils {

    public static MessageAddedEvent messageAddedEvent() {
        return new MessageAddedEvent(
                "1243A",
                MessageDirection.BUYER_TO_SELLER,
                new DateTime("2015-10-12"),
                MessageState.SENT,
                "messageIdHeader",
                "inResponseToMessageId",
                FilterResultState.OK,
                ModerationResultState.UNCHECKED,
                ImmutableMap.of(),
                "Text",
                Collections.<String>emptyList());
    }

    public static MessageAddedEvent messageAddedEventWithMessageInBlockedState() {
        return new MessageAddedEvent(
                "1243A",
                MessageDirection.BUYER_TO_SELLER,
                new DateTime(),
                MessageState.BLOCKED,
                "messageIdHeader",
                "inResponseToMessageId",
                FilterResultState.OK,
                ModerationResultState.UNCHECKED,
                ImmutableMap.of(),
                "Text",
                Collections.<String>emptyList());
    }

    public static MessageFilteredEvent messageFilteredEvent() {
        return new MessageFilteredEvent("1234A", new DateTime(), FilterResultState.OK, ImmutableList.of());
    }

}
