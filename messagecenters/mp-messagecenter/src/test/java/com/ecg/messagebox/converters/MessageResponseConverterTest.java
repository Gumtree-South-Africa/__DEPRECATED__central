package com.ecg.messagebox.converters;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.ParticipantRole;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.ecg.messagecenter.util.MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MessageResponseConverterTest {

    private static final String SENDER_ID = "1234", RECEIVER_ID = "5678";
    private static final String SENDER_EMAIL = "buyer@email.com", RECEIVER_EMAIL = "seller@email.com";

    private static final UUID MSG_ID = UUIDs.timeBased();
    private static final String MSG_TEXT = "message text";
    private static final Message MESSAGE = new Message(MSG_ID, MSG_TEXT, SENDER_ID, MessageType.CHAT);

    private static final List<Participant> PARTICIPANTS = Arrays.asList(
            new Participant(SENDER_ID, "userName1", SENDER_EMAIL, ParticipantRole.BUYER),
            new Participant(RECEIVER_ID, "userName2", RECEIVER_EMAIL, ParticipantRole.SELLER));

    private MessageResponseConverter messageResponseConverter = new MessageResponseConverter();

    @Test
    public void toMessageResponseForSender() {
        MessageResponse expected = newMessageResponse(MailTypeRts.OUTBOUND);
        MessageResponse actual = messageResponseConverter.toMessageResponse(MESSAGE, SENDER_ID, PARTICIPANTS);
        assertThat(actual, is(expected));
    }

    @Test
    public void toMessageResponseForReceiver() {
        MessageResponse expected = newMessageResponse(MailTypeRts.INBOUND);
        MessageResponse actual = messageResponseConverter.toMessageResponse(MESSAGE, RECEIVER_ID, PARTICIPANTS);
        assertThat(actual, is(expected));
    }

    private MessageResponse newMessageResponse(MailTypeRts boundness) {
        return new MessageResponse(
                toFormattedTimeISO8601ExplicitTimezoneOffset(new DateTime(UUIDs.unixTimestamp(MSG_ID))),
                boundness,
                MSG_TEXT,
                SENDER_EMAIL);
    }
}