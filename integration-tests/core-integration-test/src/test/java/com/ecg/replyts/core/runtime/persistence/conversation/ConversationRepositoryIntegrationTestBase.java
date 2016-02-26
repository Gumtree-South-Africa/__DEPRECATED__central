package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.command.*;
import com.ecg.replyts.core.api.model.conversation.event.ConversationDeletedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewDeadConversationCommand;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;

abstract public class ConversationRepositoryIntegrationTestBase<R extends MutableConversationRepository> {

    protected R conversationRepository;

    protected String conversationId1;
    protected String conversationId2;
    protected String conversationId1SellerSecret;
    protected String conversationId1BuyerSecret;
    protected String conversationId2SellerSecret;
    protected String conversationId2BuyerSecret;
    protected List<ConversationEvent> emittedEvents = new ArrayList<>();

    protected ConversationEventListeners conversationEventListeners =
            new ConversationEventListeners(singletonList(
                    (conversation, conversationEvents) -> emittedEvents.addAll(conversationEvents)));

    @Before
    public void generateConversationIds() {
        conversationId1 = UUID.randomUUID().toString();
        conversationId2 = UUID.randomUUID().toString();
        conversationId1SellerSecret = UUID.randomUUID().toString();
        conversationId1BuyerSecret = UUID.randomUUID().toString();
        conversationId2SellerSecret = UUID.randomUUID().toString();
        conversationId2BuyerSecret = UUID.randomUUID().toString();
        this.conversationRepository = createConversationRepository();
        emittedEvents.clear();
    }

    protected abstract R createConversationRepository();

    // TODO: test ConversationRepository.listConversationsModifiedBetween.
    // TODO: test ConversationRepository.findExistingConversationFor.

    @Test
    public void persistsDeadOnArrivalConversationsWithoutSecrets() {
        NewConversationCommand newConversation = aNewDeadConversationCommand("99998888").build();
        DefaultMutableConversation deadConversation = DefaultMutableConversation.create(newConversation);
        deadConversation.commit(conversationRepository, conversationEventListeners);
        MutableConversation read = conversationRepository.getById("99998888");
        assertEquals(ConversationState.DEAD_ON_ARRIVAL, read.getState());
        conversationRepository.deleteConversation(read);
    }

    @Test
    public void testGetById() throws Exception {
        givenABunchOfCommands();

        Conversation c = conversationRepository.getById(conversationId1);
        assertThat(c.getId(), is(conversationId1));
        assertThat(c.getAdId(), is("m123456"));
        assertThat(c.getBuyerId(), is("buyer@hotmail.com"));
        assertThat(c.getBuyerSecret(), is(conversationId1BuyerSecret));
        assertThat(c.getSellerId(), is("seller@gmail.com"));
        assertThat(c.getSellerSecret(), is(conversationId1SellerSecret));
        assertThat(c.getCreatedAt().toDateTime(DateTimeZone.UTC), is(new DateTime(2012, 2, 10, 9, 11, 43).toDateTime(DateTimeZone.UTC)));
        assertThat(c.getState(), is(ConversationState.ACTIVE));
        assertThat(c.getCustomValues().get("L1-CATEGORY-ID"), is("41"));
        assertThat(c.getLastModifiedAt().toDateTime(DateTimeZone.UTC), is(new DateTime(2012, 2, 10, 10, 30, 0).toDateTime(DateTimeZone.UTC)));
        assertThat(c.getMessageById("5678"), is(notNullValue(Message.class)));
        assertThat(c.getMessageById("9876"), is(nullValue(Message.class)));
        assertThat(c.getMessages().size(), is(2));

        Message m = c.getMessageById("9277");
        assertThat(m.getId(), is("9277"));
        assertThat(m.getMessageDirection(), is(MessageDirection.SELLER_TO_BUYER));
        assertThat(m.getReceivedAt().toDateTime(DateTimeZone.UTC), is(new DateTime(2012, 2, 10, 10, 10, 0).toDateTime(DateTimeZone.UTC)));
        assertThat(m.getHeaders().get("To"), is("9y3k9x6cvm8dp@platform.ebay.com"));
        assertThat(m.getFilterResultState(), is(FilterResultState.HELD));
        assertThat(m.getHumanResultState(), is(ModerationResultState.BAD));
        // TODO: assertThat(m.getState(), is(MessageState.BAD));
        assertThat(m.getLastModifiedAt().toDateTime(DateTimeZone.UTC), is(new DateTime(2012, 2, 10, 10, 30, 0).toDateTime(DateTimeZone.UTC)));
    }

    @Test
    public void testGetBySecret() throws Exception {
        givenABunchOfCommands();

        Conversation c = conversationRepository.getBySecret(conversationId2BuyerSecret);
        assertThat(c.getId(), is(conversationId2));
    }

    @Test
    public void testIsSecretAvailable() throws Exception {
        givenABunchOfCommands();

        assertThat(conversationRepository.isSecretAvailable(conversationId1BuyerSecret), is(false));
        assertThat(conversationRepository.isSecretAvailable("unused_secret"), is(true));
    }

    void givenABunchOfCommands() {
        given(
                NewConversationCommandBuilder.aNewConversationCommand(conversationId1).
                        withAdId("m123456").
                        withBuyer("buyer@hotmail.com", conversationId1BuyerSecret).
                        withSeller("seller@gmail.com", conversationId1SellerSecret).
                        withCreatedAt(new DateTime(2012, 2, 10, 9, 11, 43)).
                        withState(ConversationState.ACTIVE).
                        addCustomValue("L1-CATEGORY-ID", "41").
                        build(),
                AddMessageCommandBuilder.anAddMessageCommand(conversationId1, "5678").
                        withMessageDirection(MessageDirection.BUYER_TO_SELLER).
                        withReceivedAt(new DateTime(2012, 2, 10, 9, 11, 43)).
                        addHeader("To", "buyer@hotmail.com").
                        addHeader("From", "seller@gmail.com").
                        withPlainTextBody("").
                        build(),
                AddMessageCommandBuilder.anAddMessageCommand(conversationId1, "9277").
                        withMessageDirection(MessageDirection.SELLER_TO_BUYER).
                        withReceivedAt(new DateTime(2012, 2, 10, 10, 10, 0)).
                        addHeader("From", "buyer@hotmail.com").
                        addHeader("To", "9y3k9x6cvm8dp@platform.ebay.com").
                        withPlainTextBody("").
                        build(),
                MessageFilteredCommandBuilder.aMessageFilteredCommand(conversationId1, "9277").
                        withState(FilterResultState.HELD).
                        withDecidedAt(new DateTime(2012, 2, 10, 10, 20, 0)).
                        withProcessingFeedback(new ArrayList<>()).
                        build(),
                new MessageModeratedCommand(conversationId1, "9277",
                        new DateTime(2012, 2, 10, 10, 30, 0),
                        new ModerationAction(ModerationResultState.BAD, Optional.of("foo")))
        );
        given(
                NewConversationCommandBuilder.aNewConversationCommand(conversationId2).
                        withAdId("m74732").
                        withBuyer("john@ymail.com", conversationId2BuyerSecret).
                        withSeller("mary@gmail.com", conversationId2SellerSecret).
                        withCreatedAt(new DateTime(2012, 2, 10, 9, 21, 45)).
                        withState(ConversationState.ACTIVE).
                        build()
        );
    }

    private void given(ConversationCommand... commands) {
        DefaultMutableConversation conv = DefaultMutableConversation.create((NewConversationCommand) commands[0]);
        for (int i = 1; i < commands.length; i++) {
            conv.applyCommand(commands[i]);
        }
        conv.commit(conversationRepository, conversationEventListeners);
    }

    @Test
    public void removesUnusedSecretsWhenConversationGetsDeleted() throws Exception {
        givenABunchOfCommands();
        Conversation conversation1 = conversationRepository.getById(conversationId1);
        conversationRepository.deleteConversation(conversation1);

        assertNull(conversationRepository.getBySecret(conversationId1BuyerSecret));
        assertNull(conversationRepository.getBySecret(conversationId1SellerSecret));
    }

    @Test
    public void deleteConversationCommandDeletesTheCommand() {
        givenABunchOfCommands();
        DefaultMutableConversation conversation1 = (DefaultMutableConversation) conversationRepository.getById(conversationId1);
        conversation1.applyCommand(new ConversationDeletedCommand(conversationId1, now()));
        conversation1.commit(this.conversationRepository, conversationEventListeners);

        assertNull(conversationRepository.getById(conversationId1));
        assertEventOfTypeEmitted(ConversationDeletedEvent.class);
    }

    @Test
    public void listsConversationsModifiedBefore() throws Exception {
        givenABunchOfCommands();

        List<String> conversationIDs = conversationRepository.listConversationsModifiedBefore(new DateTime(2012, 2, 10, 9, 22, 0), 1);

        assertThat(conversationIDs, hasSize(1));
        assertThat(conversationIDs.get(0), is(conversationId2));
    }

    private void assertEventOfTypeEmitted(Class<? extends ConversationEvent> conversationEventClass) {
        if (emittedEvents.stream().noneMatch(conversationEvent -> conversationEventClass.isAssignableFrom(conversationEvent.getClass()))) {
            fail("No event of type " + conversationEventClass.getSimpleName() + " was emitted");
        }
    }

    public R getConversationRepository() {
        return conversationRepository;
    }
}
