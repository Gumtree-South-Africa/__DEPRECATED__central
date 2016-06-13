package com.ecg.replyts.core.runtime.model.conversation;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.command.ConversationClosedCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.conversation.event.*;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author mhuttar
 */
public class ImmutableConversationTest {
    NewConversationCommand newConversationCommand = new NewConversationCommand(
            "cid1",
            "adid1",
            "buyer1@host.com",
            "seller1@host.com",
            "secret1",
            "secret2",
            new DateTime(),
            ConversationState.ACTIVE,
            ImmutableMap.of());

    private ImmutableConversation conv;

    private MessageAddedEvent messageAddedEvent1 = new MessageAddedEvent(
            "msgid1",
            MessageDirection.BUYER_TO_SELLER,
            new DateTime(),
            MessageState.UNDECIDED,
            "<hosd89puawh4hhwef@platform.com>",
            null,
            FilterResultState.OK,
            ModerationResultState.UNCHECKED,
            ImmutableMap.of(),
            "messagebody",
            Collections.emptyList(),
            Collections.emptyList()
    );

    private MessageAddedEvent messageAddedEvent2 = new MessageAddedEvent(
            "msgid2",
            MessageDirection.SELLER_TO_BUYER,
            new DateTime(),
            MessageState.UNDECIDED,
            "<pd09gsoijghleiwhg@platform.com",
            "msgid1",
            FilterResultState.OK,
            ModerationResultState.UNCHECKED,
            ImmutableMap.of(),
            "messagebody",
            Collections.emptyList(),
            Collections.emptyList()
    );

    private MessageFilteredEvent message1Filtered = new MessageFilteredEvent("msgid1", new DateTime(), FilterResultState.DROPPED, Collections.emptyList());
    private MessageTerminatedEvent message1Terminated = new MessageTerminatedEvent("msgid1", new DateTime(), "noreason", "system", MessageState.BLOCKED);
    private MessageModeratedEvent message1Moderated = new MessageModeratedEvent("msgid1", new DateTime(), ModerationResultState.GOOD, "me");

    @Before
    public void setUp() throws Exception {
        List<ConversationEvent> apply = ImmutableConversation.apply(newConversationCommand);
        conv = ImmutableConversation.replay(apply);
    }

    @Test
    public void createNewConversation() throws Exception {

        assertEquals("cid1", conv.getId());
        assertEquals("adid1", conv.getAdId());
        assertEquals("buyer1@host.com", conv.getBuyerId());
        assertEquals("secret1", conv.getBuyerSecret());

        assertEquals("seller1@host.com", conv.getSellerId());
        assertEquals("secret2", conv.getSellerSecret());

        assertEquals(ConversationState.ACTIVE, conv.getState());
    }


    @Test
    public void closeConversation() {
        apply(new ConversationClosedEvent(new ConversationClosedCommand("cid1", ConversationRole.Buyer, DateTime.now())));

        assertEquals(ConversationState.CLOSED, conv.getState());
        assertTrue(conv.isClosedBy(ConversationRole.Buyer));

    }

    @Test
    public void addCustomVariable() {
        apply(new CustomValueAddedEvent("new-custom-value-key", "value"));
        assertEquals("value", conv.getCustomValues().get("new-custom-value-key"));
    }

    @Test
    public void addingCustomVariableLaterLeavesMessagesOkay() {
        apply(messageAddedEvent1);
        apply(new CustomValueAddedEvent("new-custom-value-key", "value"));
        assertEquals("value", conv.getCustomValues().get("new-custom-value-key"));
        assertEquals(1, conv.getMessages().size());
    }

    @Test
    public void addsMessageToConversation() throws Exception {
        apply(messageAddedEvent1);

        assertEquals(conv.getMessages().size(), 1);
        Message message = conv.getMessageById("msgid1");
        assertNotNull(message);
        assertEquals("msgid1", message.getId());
        assertEquals(MessageDirection.BUYER_TO_SELLER, message.getMessageDirection());
        assertEquals(MessageState.UNDECIDED, message.getState());
        assertEquals("<hosd89puawh4hhwef@platform.com>", message.getSenderMessageIdHeader());
        assertEquals(null, message.getInResponseToMessageId());
        assertEquals(FilterResultState.OK, message.getFilterResultState());
        assertEquals("messagebody", message.getPlainTextBody());
    }

    @Test
    public void addsSecondMessage() throws Exception {
        apply(messageAddedEvent1);

        apply(messageAddedEvent2);

        assertNotNull(conv.getMessageById("msgid2"));

        Message message2 = conv.getMessages().get(1);
        assertEquals("msgid2", message2.getId());
        assertEquals("msgid1", message2.getInResponseToMessageId());
    }

    @Test
    public void addsFilteringInformationToMessage() throws Exception {
        apply(messageAddedEvent1);
        apply(new MessageFilteredEvent(
                "msgid1",
                new DateTime(),
                FilterResultState.DROPPED,
                singletonList((ProcessingFeedback) new ImmutableProcessingFeedback("foo", "fooinst", "uiint", "desc", 100, FilterResultState.OK, false))));

        assertEquals(MessageState.UNDECIDED, conv.getMessages().get(0).getState());
        assertEquals(FilterResultState.DROPPED, conv.getMessages().get(0).getFilterResultState());
        assertEquals(1, conv.getMessages().get(0).getProcessingFeedback().size());
    }

    @Test
    public void mergesProcessingFeedback() throws Exception {
        apply(messageAddedEvent1);
        apply(new MessageFilteredEvent(
                "msgid1",
                new DateTime(),
                FilterResultState.OK,
                singletonList((ProcessingFeedback) new ImmutableProcessingFeedback("foo1", "fooinst", "uiint", "desc", 100, FilterResultState.OK, false))));

        apply(new MessageFilteredEvent(
                "msgid1",
                new DateTime(),
                FilterResultState.HELD,
                singletonList((ProcessingFeedback) new ImmutableProcessingFeedback("foo2", "fooinst", "uiint", "desc", 100, FilterResultState.OK, false))));

        assertEquals(MessageState.UNDECIDED, conv.getMessages().get(0).getState());
        assertEquals(FilterResultState.HELD, conv.getMessages().get(0).getFilterResultState());
        assertEquals(2, conv.getMessages().get(0).getProcessingFeedback().size());
    }

    @Test
    public void addsProcessingFeedbackToDifferentMessages() throws Exception {
        apply(messageAddedEvent1);
        apply(messageAddedEvent2);
        apply(new MessageFilteredEvent(
                "msgid1",
                new DateTime(),
                FilterResultState.OK,
                singletonList((ProcessingFeedback) new ImmutableProcessingFeedback("foo1", "fooinst", "uiint", "desc", 100, FilterResultState.OK, false))));

        apply(new MessageFilteredEvent(
                "msgid2",
                new DateTime(),
                FilterResultState.HELD,
                singletonList((ProcessingFeedback) new ImmutableProcessingFeedback("foo2", "fooinst", "uiint", "desc", 100, FilterResultState.OK, false))));

        assertEquals(MessageState.UNDECIDED, conv.getMessages().get(0).getState());

        assertEquals(FilterResultState.OK, conv.getMessages().get(0).getFilterResultState());
        assertEquals(1, conv.getMessages().get(0).getProcessingFeedback().size());
        assertEquals("foo1", conv.getMessages().get(0).getProcessingFeedback().get(0).getFilterName());

        assertEquals(FilterResultState.HELD, conv.getMessages().get(1).getFilterResultState());
        assertEquals(1, conv.getMessages().get(1).getProcessingFeedback().size());
        assertEquals("foo2", conv.getMessages().get(1).getProcessingFeedback().get(0).getFilterName());
    }


    @Test(expected = NullPointerException.class)
    public void rejectsToAddFilteredEventToNonexistingMessage() throws Exception {
        apply(new MessageFilteredEvent(
                "msgid1",
                new DateTime(),
                FilterResultState.OK,
                singletonList((ProcessingFeedback) new ImmutableProcessingFeedback("foo1", "fooinst", "uiint", "desc", 100, FilterResultState.OK, false))));
    }


    @Test
    public void terminatesMessages() throws Exception {
        apply(messageAddedEvent1);
        apply(new MessageTerminatedEvent("msgid1", new DateTime(), "finished", "me", MessageState.BLOCKED));

        Message m = conv.getMessageById("msgid1");
        assertEquals(MessageState.BLOCKED, m.getState());
        assertEquals(1, m.getProcessingFeedback().size());
    }


    @Test
    public void mergesProcessingFeedbacksOnTermination() throws Exception {
        apply(messageAddedEvent1);
        apply(new MessageFilteredEvent(
                "msgid1",
                new DateTime(),
                FilterResultState.HELD,
                singletonList((ProcessingFeedback) new ImmutableProcessingFeedback("foo2", "fooinst", "uiint", "desc", 100, FilterResultState.OK, false))));

        apply(new MessageTerminatedEvent("msgid1", new DateTime(), "finished", "me", MessageState.BLOCKED));


        Message m = conv.getMessageById("msgid1");
        assertEquals(MessageState.BLOCKED, m.getState());
        assertEquals(2, m.getProcessingFeedback().size());

    }

    @Test
    public void messageInSentAfterModerationWithGood() throws Exception {
        apply(messageAddedEvent1);
        apply(new MessageTerminatedEvent(messageAddedEvent1.getMessageId(), DateTime.now(), "foo", "foo", MessageState.HELD));

        assertEquals(MessageState.HELD, conv.getMessageById(messageAddedEvent1.getMessageId()).getState());
        apply(new MessageModeratedEvent(messageAddedEvent1.getMessageId(), DateTime.now(), ModerationResultState.GOOD, null));

        Message message = conv.getMessageById(messageAddedEvent1.getMessageId());
        assertEquals(MessageState.SENT, message.getState());
    }

    @Test
    public void lastEditorSetWhenPresent() throws Exception {
        apply(messageAddedEvent1);
        apply(new MessageTerminatedEvent(messageAddedEvent1.getMessageId(), DateTime.now(), "foo", "foo", MessageState.HELD));

        assertEquals(MessageState.HELD, conv.getMessageById(messageAddedEvent1.getMessageId()).getState());
        apply(new MessageModeratedEvent(messageAddedEvent1.getMessageId(), DateTime.now(), ModerationResultState.GOOD, "smith"));

        Message message = conv.getMessageById(messageAddedEvent1.getMessageId());
        assertEquals("smith", message.getLastEditor().get());

    }

    @Test
    public void versionsForNewConversationIsZero() {
        assertEquals(0, conv.getVersion());

    }

    @Test
    public void versionsForConversationAfterOneEventIsOne() {
        apply(messageAddedEvent1);
        assertEquals(1, conv.getVersion());

    }

    @Test
    public void versionsForConversationAfterTwoEventsIsTwo() {
        apply(messageAddedEvent1);
        apply(messageAddedEvent2);
        assertEquals(2, conv.getVersion());

    }

    @Test
    public void versionForMessageIsZeroAfterCreation() {
        apply(messageAddedEvent1);

        assertEquals(0, conv.getMessages().get(0).getVersion());

    }

    @Test
    public void versionsForMEssageIsIncreasedAfterWorkflow() {
        apply(messageAddedEvent1);
        apply(message1Filtered);
        apply(message1Terminated);
        apply(message1Moderated);

        assertEquals(3, conv.getMessages().get(0).getVersion());

    }

    @Test
    public void messageVersionsAreIsolated() {
        apply(messageAddedEvent1);
        apply(messageAddedEvent2);
        apply(message1Filtered);
        apply(message1Terminated);
        apply(message1Moderated);

        assertEquals(3, conv.getMessageById("msgid1").getVersion());
        assertEquals(0, conv.getMessageById("msgid2").getVersion());

    }

    @Test
    public void conversationVersionIsIncreasedByMessageChanges() {

        apply(messageAddedEvent1);
        apply(messageAddedEvent2);
        apply(message1Filtered);
        apply(message1Terminated);
        apply(message1Moderated);

        assertEquals(5, conv.getVersion());
    }

    private void apply(ConversationEvent event) {
        conv = conv.updateMany(singletonList(event));
    }
}
