package ca.kijiji.replyts.user_behaviour.responsiveness;

import ca.kijiji.replyts.BoxHeaders;
import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ResponsivenessCalculatorTest {
    static final String MSG_ID = "1";
    static final String CONV_ID = MSG_ID;
    static final String AD_ID = "12345";
    static final String REPLIER_EMAIL = "replier@kijiji.ca";
    static final String REPLIER_SECRET = "abc123";
    static final String POSTER_EMAIL = "poster@kijiji.ca";
    static final String POSTER_SECRET = "qwe321";
    static final String REPLIER_NAME = "Replier Name";
    static final String REPLIER_ID = "789";
    static final String POSTER_ID = "123";

    private ResponsivenessCalculator calculator;

    private DateTime now;
    private DateTime msgReceivedDate;
    private DateTime convCreatedDate;
    private DateTime convModifiedDate;
    private ImmutableMessage.Builder msgBuilder;
    private ImmutableConversation.Builder convBuilder;

    @Before
    public void setUp() throws Exception {
        calculator = new ResponsivenessCalculator();

        now = DateTime.now(DateTimeZone.UTC);
        msgReceivedDate = now.minusMinutes(10);
        convCreatedDate = msgReceivedDate.minusSeconds(1);
        convModifiedDate = now;
        msgBuilder = ImmutableMessage.Builder.aMessage()
                .withId(MSG_ID)
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withState(MessageState.SENT)
                .withReceivedAt(msgReceivedDate)
                .withLastModifiedAt(msgReceivedDate)
                .withTextParts(ImmutableList.of(""))
                .withHeader(BoxHeaders.REPLIER_NAME.getHeaderName(), REPLIER_NAME)
                .withHeader(BoxHeaders.REPLIER_ID.getHeaderName(), REPLIER_ID)
                .withHeader(BoxHeaders.POSTER_ID.getHeaderName(), POSTER_ID);

        convBuilder = ImmutableConversation.Builder.aConversation()
                .withId(CONV_ID)
                .withAdId(AD_ID)
                .withBuyer(REPLIER_EMAIL, REPLIER_SECRET)
                .withSeller(POSTER_EMAIL, POSTER_SECRET)
                .withState(ConversationState.ACTIVE)
                .withCreatedAt(convCreatedDate)
                .withLastModifiedAt(convModifiedDate);
    }

    @Test
    public void firstMsgInConvo_noRecord() throws Exception {
        Message message = msgBuilder.build();
        Conversation conversation = convBuilder.withMessages(ImmutableList.of(message)).build();

        ResponsivenessRecord record = calculator.calculateResponsiveness(conversation, message);

        assertThat(record, nullValue());
    }

    @Test
    public void initialResponse_reported() throws Exception {
        Message initialMsg = msgBuilder.build();
        Message response = msgBuilder
                .withId("2")
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withReceivedAt(msgReceivedDate.plusSeconds(600))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(600))
                .withHeaders(ImmutableMap.of())
                .build();

        Conversation conversation = convBuilder.withMessages(
                ImmutableList.of(initialMsg, response)
        ).build();

        ResponsivenessRecord record = calculator.calculateResponsiveness(conversation, response);

        assertThat(record.getUserId(), equalTo(Long.valueOf(POSTER_ID)));
        assertThat(record.getTimeToRespondInSeconds(), equalTo(600));
        assertThat(record.getConversationId(), equalTo(CONV_ID));
        assertThat(record.getVersion(), equalTo(ResponsivenessCalculator.VERSION));
        assertThat(record.getMessageId(), equalTo("2"));
    }

    @Test
    public void multipleFollowUpsFromSameParty_timeReportedFromLastResponse() throws Exception {
        Message initialMsg = msgBuilder.build();
        Message followUp1 = msgBuilder
                .withId("2")
                .withReceivedAt(msgReceivedDate.plusSeconds(30))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(30))
                .build();
        Message followUp2 = msgBuilder
                .withId("3")
                .withReceivedAt(msgReceivedDate.plusSeconds(60))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(60))
                .build();
        Message response = msgBuilder
                .withId("4")
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withReceivedAt(msgReceivedDate.plusSeconds(600))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(600))
                .withHeaders(ImmutableMap.of())
                .build();

        Conversation conversation = convBuilder.withMessages(
                ImmutableList.of(initialMsg, followUp1, followUp2, response)
        ).build();

        ResponsivenessRecord record = calculator.calculateResponsiveness(conversation, response);

        assertThat(record.getUserId(), equalTo(Long.valueOf(POSTER_ID)));
        assertThat(record.getTimeToRespondInSeconds(), equalTo(600 - 60));
    }

    @Test
    public void messageIsHeld_notReported() throws Exception {
        Message initialMsg = msgBuilder.build();
        Message response = msgBuilder
                .withId("2")
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withReceivedAt(msgReceivedDate.plusSeconds(600))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(600))
                .withState(MessageState.HELD)
                .withHeaders(ImmutableMap.of())
                .build();

        Conversation conversation = convBuilder.withMessages(
                ImmutableList.of(initialMsg, response)
        ).build();

        ResponsivenessRecord record = calculator.calculateResponsiveness(conversation, response);

        assertThat(record, nullValue());
    }

    @Test
    public void sellerRespondsToAnonBuyer_reported() throws Exception {
        Message initialMsg = msgBuilder
                .withHeaders(ImmutableMap.of(
                        BoxHeaders.REPLIER_NAME.getHeaderName(), REPLIER_NAME,
                        BoxHeaders.POSTER_ID.getHeaderName(), POSTER_ID
                ))
                .build();
        Message sellerResponse = msgBuilder
                .withId("2")
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withReceivedAt(msgReceivedDate.plusSeconds(600))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(600))
                .withHeaders(ImmutableMap.of())
                .build();

        Conversation conversation = convBuilder.withMessages(
                ImmutableList.of(initialMsg, sellerResponse)
        ).build();

        ResponsivenessRecord record = calculator.calculateResponsiveness(conversation, sellerResponse);

        assertThat(record.getUserId(), equalTo(Long.valueOf(POSTER_ID)));
        assertThat(record.getTimeToRespondInSeconds(), equalTo(600));
    }

    @Test
    public void anonBuyerResponds_notReported() throws Exception {
        Message initialMsg = msgBuilder
                .withHeaders(ImmutableMap.of(
                        BoxHeaders.REPLIER_NAME.getHeaderName(), REPLIER_NAME,
                        BoxHeaders.POSTER_ID.getHeaderName(), POSTER_ID
                ))
                .build();
        Message sellerResponse = msgBuilder
                .withId("2")
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withReceivedAt(msgReceivedDate.plusSeconds(600))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(600))
                .withHeaders(ImmutableMap.of())
                .build();
        Message anonBuyerResponse = msgBuilder
                .withId("3")
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withReceivedAt(msgReceivedDate.plusSeconds(600 * 2))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(600 * 2))
                .build();

        Conversation conversation = convBuilder.withMessages(
                ImmutableList.of(initialMsg, sellerResponse, anonBuyerResponse)
        ).build();

        ResponsivenessRecord record = calculator.calculateResponsiveness(conversation, anonBuyerResponse);

        assertThat(record, nullValue());
    }

    @Test
    public void ourMsgNotInConvo_notReported() throws Exception {
        // Weirder things have happened
        Message initialMsg = msgBuilder
                .withHeaders(ImmutableMap.of(
                        BoxHeaders.REPLIER_NAME.getHeaderName(), REPLIER_NAME,
                        BoxHeaders.POSTER_ID.getHeaderName(), POSTER_ID
                ))
                .build();
        Message ourResponse = msgBuilder
                .withId("2")
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withReceivedAt(msgReceivedDate.plusSeconds(600))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(600))
                .withHeaders(ImmutableMap.of())
                .build();

        Conversation conversation = convBuilder.withMessages(
                ImmutableList.of(initialMsg)
        ).build();

        ResponsivenessRecord record = calculator.calculateResponsiveness(conversation, ourResponse);

        assertThat(record, nullValue());
    }

    @Test
    public void conversationLimitReached_notReported() throws Exception {
        Message initialMsg = msgBuilder
                .withHeaders(ImmutableMap.of(
                        BoxHeaders.REPLIER_NAME.getHeaderName(), REPLIER_NAME,
                        BoxHeaders.POSTER_ID.getHeaderName(), POSTER_ID
                ))
                .build();
        Message followUp = msgBuilder
                .withHeaders(ImmutableMap.of())
                .withId("followUp0")
                .build();

        List<Message> messageList = new ArrayList<>();
        messageList.add(initialMsg);
        messageList.add(followUp);

        for (int i = 1; i < 499; i++) {
            messageList.add(msgBuilder
                    .withHeaders(ImmutableMap.of())
                    .withId("followUp" + i)
                    .build());
        }

        Conversation conversation = convBuilder.withMessages(
                ImmutableList.copyOf(messageList)
        ).build();

        assertThat(conversation.getMessages().size(), equalTo(500));

        Message ourResponse = msgBuilder
                .withId("2")
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withReceivedAt(msgReceivedDate.plusSeconds(600))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(600))
                .withHeaders(ImmutableMap.of())
                .build();

        ResponsivenessRecord record = calculator.calculateResponsiveness(conversation, ourResponse);

        assertThat(record, nullValue());
    }

    @Test
    public void heldMessageSentAfterOtherPartyResponds_recorded() throws Exception {
        Message initial = msgBuilder.build();
        Message sellerReply = msgBuilder
                .withId("2")
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withReceivedAt(msgReceivedDate.plusSeconds(600))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(600))
                .withHeaders(ImmutableMap.of())
                .build();
        Message buyerReplyHeldThenReleased = msgBuilder
                .withId("3")
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withReceivedAt(msgReceivedDate.plusSeconds(1200))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(2400))
                .withFilterResultState(FilterResultState.HELD)
                .withHumanResultState(ModerationResultState.GOOD)
                .build();
        Message sellerReplyAfterBuyerHeld = msgBuilder
                .withId("4")
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withReceivedAt(msgReceivedDate.plusSeconds(1800))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(1800))
                .withFilterResultState(FilterResultState.OK)
                .withHumanResultState(ModerationResultState.UNCHECKED)
                .build();

        Conversation conversation = convBuilder.withMessages(
                ImmutableList.of(initial, sellerReply, buyerReplyHeldThenReleased, sellerReplyAfterBuyerHeld)
        ).build();

        ResponsivenessRecord record = calculator.calculateResponsiveness(conversation, buyerReplyHeldThenReleased);

        assertThat(record.getUserId(), equalTo(Long.valueOf(REPLIER_ID)));
        assertThat(record.getTimeToRespondInSeconds(), equalTo(600));
    }

    @Test
    public void sellerRespondsBeforeAndAfterBuyersHeldMsg_notReported() throws Exception {
        Message initial = msgBuilder.build();
        Message sellerReply = msgBuilder
                .withId("2")
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withReceivedAt(msgReceivedDate.plusSeconds(600))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(600))
                .withHeaders(ImmutableMap.of())
                .build();
        Message buyerReplyHeld = msgBuilder
                .withId("3")
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withState(MessageState.HELD)
                .withReceivedAt(msgReceivedDate.plusSeconds(1200))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(1200))
                .build();
        Message sellerReplyAfterBuyerHeld = msgBuilder
                .withId("4")
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withState(MessageState.SENT)
                .withReceivedAt(msgReceivedDate.plusSeconds(1800))
                .withLastModifiedAt(msgReceivedDate.plusSeconds(1800))
                .withFilterResultState(FilterResultState.OK)
                .withHumanResultState(ModerationResultState.UNCHECKED)
                .build();

        Conversation conversation = convBuilder.withMessages(
                ImmutableList.of(initial, sellerReply, buyerReplyHeld, sellerReplyAfterBuyerHeld)
        ).build();

        ResponsivenessRecord record = calculator.calculateResponsiveness(conversation, sellerReplyAfterBuyerHeld);

        assertThat(record, nullValue());
    }

    @Test
    public void everyMessageHeldAndTimedOut_unseenMsgInBetweenResponsesSkipped() throws Exception {
        /*
         * Seller S is talking to buyer B. Buyer B's email address has a delay score, so every email (regardless
         * of the side) gets delayed. Sequence of events:
         * (1) B->S at time t1. Gets delayed until time t4
         * (2) S->B at time t5. Gets delayed until time t9
         * (3) B->S at time t6. Gets delayed until time t10
         *
         * The message (3) from B is a reply not to message (2) from S above, but to one before (1), because B couldn't
         * have seen (2) at the time that (3) was sent.
         *
         * KCA-16792
         */

        Message initial = msgBuilder
                .withState(MessageState.SENT)
                .withFilterResultState(FilterResultState.HELD)
                .withLastModifiedAt(msgReceivedDate.plusHours(4))
                .build();
        Message sellerReply = msgBuilder
                .withId("2")
                .withState(MessageState.SENT)
                .withFilterResultState(FilterResultState.HELD)
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withReceivedAt(msgReceivedDate.plusHours(5))
                .withLastModifiedAt(msgReceivedDate.plusHours(9))
                .withHeaders(ImmutableMap.of())
                .build();
        Message buyerReply = msgBuilder
                .withId("3")
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withState(MessageState.SENT)
                .withFilterResultState(FilterResultState.HELD)
                .withReceivedAt(msgReceivedDate.plusHours(6))
                .withLastModifiedAt(msgReceivedDate.plusHours(10))
                .build();

        Conversation conversation = convBuilder.withMessages(
                ImmutableList.of(initial, sellerReply, buyerReply)
        ).build();

        ResponsivenessRecord record = calculator.calculateResponsiveness(conversation, buyerReply);
        assertNull(record);
    }
}
