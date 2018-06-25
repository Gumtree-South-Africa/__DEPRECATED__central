package com.ecg.comaas.mp.filter.volume;

import com.ecg.comaas.mp.filter.volume.VolumeFilterConfiguration.VolumeRule;
import com.ecg.comaas.mp.filter.volume.persistence.VolumeFilterEventRepository;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.ecg.comaas.mp.filter.volume.VolumeFilter.wasPreviousMessageBySameUserDroppedOrHeld;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static com.ecg.replyts.core.api.model.conversation.MessageState.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class VolumeFilterTest {
    private static final String BUYER_ID = "buyer@mail.com";

    private VolumeRule VOLUME_RULE_1_HOUR = new VolumeRule(1L, TimeUnit.HOURS, 100L, 200);
    private VolumeRule VOLUME_RULE_2_DAY = new VolumeRule(1L, TimeUnit.DAYS, 200L, 100);
    private List<VolumeRule> VOLUME_RULES = asList(VOLUME_RULE_2_DAY, VOLUME_RULE_1_HOUR);

    private VolumeFilter volumeFilter;
    private MessageProcessingContext messageProcessingContext;

    private final int TTL = 24 * 60 * 60;

    private final int SECONDS_FOR_RULE1 = 60 * 60;
    private final int SECONDS_FOR_RULE2 = 24 * 60 * 60;

    private final Message message = mock(Message.class);
    private final Message firstMessage = mock(Message.class);
    private final MutableConversation conversation = mock(MutableConversation.class);
    private final Mail mail = mock(Mail.class);
    private final VolumeFilterEventRepository vts = mock(VolumeFilterEventRepository.class);

    @Before
    public void setUp() throws Exception {
        VolumeFilterConfiguration configuration = new VolumeFilterConfiguration(VOLUME_RULES);
        volumeFilter = new VolumeFilter(configuration, vts);
        messageProcessingContext = mockMessageProcessingContext(mail, message, conversation);
    }

    @Test
    public void filterRecordsFirstMailInConversation() throws Exception {
        setUpFirstMailInConversation();
        volumeFilter.filter(messageProcessingContext);
        verify(vts).record(BUYER_ID, TTL);
    }

    @Test
    public void filterReturnsEmptyListIfNoRuleIsViolated() throws Exception {
        setUpFirstMailInConversation();
        List<FilterFeedback> filterFeedbacks = volumeFilter.filter(messageProcessingContext);
        assertThat(filterFeedbacks.size(), is(0));
        InOrder inOrder = inOrder(vts);
        inOrder.verify(vts).count(BUYER_ID, SECONDS_FOR_RULE1);
        inOrder.verify(vts).count(BUYER_ID, SECONDS_FOR_RULE2);
    }

    @Test
    public void filterUsesFirstViolatedRule() throws Exception {
        setUpFirstMailInConversation();
        when(vts.count(BUYER_ID, SECONDS_FOR_RULE1)).thenReturn(201);
        List<FilterFeedback> filterFeedbacks = volumeFilter.filter(messageProcessingContext);
        FilterFeedback filterFeedback = filterFeedbacks.get(0);
        assertThat(filterFeedback.getUiHint(), is("bla@bla.com>100 mails/1 HOURS +200"));
        assertThat(filterFeedback.getDescription(), is("bla@bla.com sent more than 100 mails the last 1 HOURS"));
        assertThat(filterFeedback.getScore(), is(200));
        assertThat(filterFeedback.getResultState(), is(FilterResultState.OK));

        verify(vts).count(BUYER_ID, SECONDS_FOR_RULE1);
        verify(vts, never()).count(BUYER_ID, SECONDS_FOR_RULE2);
    }

    @Test
    public void filterDoesNotRecordBids() throws Exception {
        setUpFirstMailInConversation();
        Map<String, String> customValues = Collections.singletonMap("flowtype", "PLACED_BID");
        when(conversation.getCustomValues()).thenReturn(customValues);
        volumeFilter.filter(messageProcessingContext);
        verify(vts, never()).record(BUYER_ID, TTL);
    }

    private void setUpFirstMailInConversation() {
        when(message.getId()).thenReturn("message-1");
        when(message.getMessageDirection()).thenReturn(BUYER_TO_SELLER);
        when(conversation.getMessages()).thenReturn(singletonList(message));
        when(conversation.getUserId(ConversationRole.Buyer)).thenReturn(BUYER_ID);
        when(mail.getReplyTo()).thenReturn("bla@bla.com");
    }

    private void setUpTwoMailsInConversation() {
        when(firstMessage.getId()).thenReturn("message-1");
        when(message.getId()).thenReturn("message-2");
        when(message.getMessageDirection()).thenReturn(BUYER_TO_SELLER);
        when(conversation.getMessages()).thenReturn(asList(firstMessage, message));
        when(conversation.getUserId(ConversationRole.Buyer)).thenReturn(BUYER_ID);
        when(mail.getFrom()).thenReturn("bla@bla.com");
    }

    private MessageProcessingContext mockMessageProcessingContext(Mail mail, Message message, MutableConversation conversation) {
        MessageProcessingContext messageProcessingContext = mock(MessageProcessingContext.class);
        when(messageProcessingContext.getConversation()).thenReturn(conversation);
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(messageProcessingContext.getMail()).thenReturn(Optional.of(mail));
        return messageProcessingContext;
    }

    private static final String BUYER_UID = "Mr.Baddie";
    private static final String SELLER_UID = "Mr.Awesome";

    @Test
    public void testWasPreviousMessageBySameUserDroppedOrHeld() {
        assertFalse(wasPreviousMessageBySameUserDroppedOrHeld(conversation(), BUYER_UID, "1"));
        assertFalse(wasPreviousMessageBySameUserDroppedOrHeld(conversation(message(SENT, BUYER_TO_SELLER)), BUYER_UID, "1"));
        assertTrue(wasPreviousMessageBySameUserDroppedOrHeld(conversation(message(BLOCKED, BUYER_TO_SELLER)), BUYER_UID, "1"));
        assertTrue(wasPreviousMessageBySameUserDroppedOrHeld(conversation(message(HELD, BUYER_TO_SELLER)), BUYER_UID, "1"));
        assertFalse(wasPreviousMessageBySameUserDroppedOrHeld(conversation(message(HELD, BUYER_TO_SELLER), message(SENT, BUYER_TO_SELLER)), BUYER_UID, "1"));
        assertFalse(wasPreviousMessageBySameUserDroppedOrHeld(conversation(message(SENT, BUYER_TO_SELLER), message(HELD, SELLER_TO_BUYER)), BUYER_UID, "1"));
        assertTrue(wasPreviousMessageBySameUserDroppedOrHeld(conversation(message(HELD, BUYER_TO_SELLER), message(SENT, SELLER_TO_BUYER)), BUYER_UID, "1"));
        assertTrue(wasPreviousMessageBySameUserDroppedOrHeld(conversation(message(BLOCKED, BUYER_TO_SELLER, "1"), message(UNDECIDED, BUYER_TO_SELLER, "2")), BUYER_UID, "2"));
    }

    private static Message message(MessageState state, MessageDirection direction) {
        return message(state, direction, "");
    }

    private static Message message(MessageState state, MessageDirection direction, String msgId) {
        Message m = mock(Message.class);
        when(m.getState()).thenReturn(state);
        when(m.getMessageDirection()).thenReturn(direction);
        when(m.getId()).thenReturn(msgId);
        return m;
    }

    private static Conversation conversation(Message... messages) {
        Conversation c = mock(Conversation.class);
        when(c.getMessages()).thenReturn(Arrays.asList(messages));
        when(c.getUserId(ConversationRole.Buyer)).thenReturn(BUYER_UID);
        when(c.getUserId(ConversationRole.Seller)).thenReturn(SELLER_UID);
        return c;
    }
}
