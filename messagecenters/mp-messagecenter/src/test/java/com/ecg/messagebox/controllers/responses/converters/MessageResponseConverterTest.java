package com.ecg.messagebox.controllers.responses.converters;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.controllers.responses.MessageResponse;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.MessageMetadata;
import com.ecg.messagebox.model.MessageType;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static com.ecg.messagecenter.util.MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MessageResponseConverterTest {

    private static final UUID MESSAGE_ID = UUIDs.timeBased();
    private static final MessageType MESSAGE_TYPE = MessageType.CHAT;
    private static final String TEXT = "text";
    private static final String SENDER_USER_ID = "user id";
    private static final String CUSTOM_DATA = "custom data";

    private MessageResponseConverter msgRespConverter;

    @Before
    public void setup() {
        msgRespConverter = new MessageResponseConverter();
    }

    @Test
    public void toMessageResponse() {
        Message message = new Message(
                MESSAGE_ID,
                MESSAGE_TYPE,
                new MessageMetadata(TEXT, SENDER_USER_ID, CUSTOM_DATA));

        MessageResponse expected = new MessageResponse(
                MESSAGE_ID.toString(),
                MESSAGE_TYPE.getValue(),
                TEXT,
                SENDER_USER_ID,
                toFormattedTimeISO8601ExplicitTimezoneOffset(message.getReceivedDate()),
                CUSTOM_DATA);

        MessageResponse actual = msgRespConverter.toMessageResponse(message);

        assertThat(actual, is(expected));
    }
}