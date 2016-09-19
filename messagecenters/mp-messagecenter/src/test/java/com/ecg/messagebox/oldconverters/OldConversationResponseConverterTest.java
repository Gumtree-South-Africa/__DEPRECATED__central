package com.ecg.messagebox.oldconverters;

import com.ecg.messagebox.model.*;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static com.datastax.driver.core.utils.UUIDs.timeBased;
import static com.ecg.messagecenter.util.MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.joda.time.DateTime.now;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OldConversationResponseConverterTest {

    private static final String BUYER_ID = "123", SELLER_ID = "456";
    private static final String BUYER_NAME = "user 1", SELLER_NAME = "user 2";
    private static final String BUYER_EMAIL = "user email 1", SELLER_EMAIL = "user email 2";

    private static final List<Participant> PARTICIPANTS = asList(
            new Participant(BUYER_ID, BUYER_NAME, BUYER_EMAIL, ParticipantRole.BUYER),
            new Participant(SELLER_ID, SELLER_NAME, SELLER_EMAIL, ParticipantRole.SELLER));

    private static final Message MSG_BUYER = new Message(timeBased(), MessageType.ASQ, new MessageMetadata("text buyer", BUYER_ID));
    private static final Message MSG_SELLER = new Message(timeBased(), MessageType.CHAT, new MessageMetadata("text seller", SELLER_ID));

    private static final String CONVERSATION_ID = "c1";
    private static final String AD_ID = "m1234";
    private static final String EMAIL_SUBJECT = "email subject";
    private static final int UNREAD_MSG_COUNT = 1;

    @Mock
    private OldMessageResponseConverter msgRespConverter;
    @Mock
    private MessageResponse msgResp;

    private OldConversationResponseConverter convRespConverter;

    @Before
    public void setup() {
        DateTimeUtils.setCurrentMillisFixed(now().getMillis());
        convRespConverter = new OldConversationResponseConverter(msgRespConverter);
        when(msgRespConverter.toMessageResponse(any(), any(), any())).thenReturn(msgResp);
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void toConversationResponseForBuyer() {
        ConversationResponse expected = newConversationResponse(ConversationRole.Buyer);

        ConversationResponse actual = convRespConverter.toConversationResponse(newConversation(), BUYER_ID);

        verify(msgRespConverter).toMessageResponse(MSG_BUYER, BUYER_ID, PARTICIPANTS);
        verify(msgRespConverter).toMessageResponse(MSG_SELLER, BUYER_ID, PARTICIPANTS);
        assertThat(actual, is(expected));
    }

    @Test
    public void toConversationResponseForSeller() {
        ConversationResponse expected = newConversationResponse(ConversationRole.Seller);

        ConversationResponse actual = convRespConverter.toConversationResponse(newConversation(), SELLER_ID);

        verify(msgRespConverter).toMessageResponse(MSG_BUYER, SELLER_ID, PARTICIPANTS);
        verify(msgRespConverter).toMessageResponse(MSG_SELLER, SELLER_ID, PARTICIPANTS);
        assertThat(actual, is(expected));
    }

    private ConversationThread newConversation() {
        return new ConversationThread(CONVERSATION_ID, AD_ID, Visibility.ACTIVE, MessageNotification.RECEIVE,
                PARTICIPANTS, MSG_SELLER, new ConversationMetadata(now(), EMAIL_SUBJECT))
                .addNumUnreadMessages(UNREAD_MSG_COUNT)
                .addMessages(asList(MSG_BUYER, MSG_SELLER));
    }

    private ConversationResponse newConversationResponse(ConversationRole role) {
        return new ConversationResponse(CONVERSATION_ID, role, BUYER_EMAIL, SELLER_EMAIL, BUYER_NAME, SELLER_NAME,
                Long.valueOf(BUYER_ID), Long.valueOf(SELLER_ID), AD_ID, toFormattedTimeISO8601ExplicitTimezoneOffset(now()),
                EMAIL_SUBJECT, asList(msgResp, msgResp), UNREAD_MSG_COUNT);
    }
}