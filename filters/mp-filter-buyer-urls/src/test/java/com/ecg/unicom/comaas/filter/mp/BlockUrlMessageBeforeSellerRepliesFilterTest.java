package com.ecg.unicom.comaas.filter.mp;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BlockUrlMessageBeforeSellerRepliesFilterTest {

    private final BlockUrlMessageBeforeSellerRepliesFilter filter = new BlockUrlMessageBeforeSellerRepliesFilter();

    @Test
    public void filteredOutWhenNoResponseFromSellerYet() {
        Conversation conversation = mock(Conversation.class);
        List<Message> messages = Arrays.asList(
                mockMessage(MessageDirection.BUYER_TO_SELLER),
                mockMessage(MessageDirection.BUYER_TO_SELLER),
                mockMessage(MessageDirection.BUYER_TO_SELLER)
        );
        when(conversation.getMessages()).thenReturn(messages);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("X-User-Message", "some message url.com");

        Message messageWithUrl = mock(Message.class);
        when(messageWithUrl.getCaseInsensitiveHeaders()).thenReturn(metadata);
        MessageProcessingContext context = mock(MessageProcessingContext.class);
        when(context.getConversation()).thenReturn(conversation);
        when(context.getMessage()).thenReturn(messageWithUrl);

        List<FilterFeedback> feedbacks = filter.filter(context);

        assertThat(feedbacks, hasSize(1));

        FilterFeedback feedback = feedbacks.get(0);
        assertThat(feedback.getResultState(), is(FilterResultState.DROPPED));
    }

    @Test
    public void notFilteredOutWhenResponseFromSellerHappened() {
        Conversation conversation = mock(Conversation.class);
        List<Message> messages = Arrays.asList(
                mockMessage(MessageDirection.BUYER_TO_SELLER),
                mockMessage(MessageDirection.SELLER_TO_BUYER),
                mockMessage(MessageDirection.BUYER_TO_SELLER)
        );
        when(conversation.getMessages()).thenReturn(messages);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("X-User-Message", "some message url.com");

        Message messageWithUrl = mock(Message.class);
        when(messageWithUrl.getCaseInsensitiveHeaders()).thenReturn(metadata);
        MessageProcessingContext context = mock(MessageProcessingContext.class);
        when(context.getConversation()).thenReturn(conversation);
        when(context.getMessage()).thenReturn(messageWithUrl);

        List<FilterFeedback> feedbacks = filter.filter(context);

        assertThat(feedbacks, hasSize(0));
    }

    @Test
    public void messageDoesntContainUrls() {
        BlockUrlMessageBeforeSellerRepliesFilter filter = new BlockUrlMessageBeforeSellerRepliesFilter();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("X-User-Message", "some message");

        Message message = mock(Message.class);
        when(message.getCaseInsensitiveHeaders()).thenReturn(metadata);

        assertThat(filter.containsUrls(message), is(false));
    }

    @Test
    public void messageContainsUrl() {
        assertContainsUrl("some message url.com");
    }

    @Test
    public void spamMessage1ContainsUrl() {
        assertContainsUrl("Веn jе het beu om ееn hеtе meid tе zоеkеn? Wil jе waаrsсhijnlijk vаnavоnd \n" +
                "sеks? Uw prоblemen wоrden hiеr орgelоst: \n" +
                "http://www.marktplaats.nl/gateway.html?url=http%3A%2F%2F7sex.nl Slесhts 18+ \n" +
                "\uD83D\uDD1E\uD83D\uDD1E ");
    }

    @Test
    public void spamMessage2ContainsUrl() {
        assertContainsUrl("Веn jе het beu om ееn hеtе meid tе zоеkеn? Wil jе waаrsсhijnlijk vаnavоnd \n" +
                "sеks? Uw prоblemen wоrden hiеr орgelоst: http://7sex.nl Slесhts 18+ \uD83D\uDD1E\uD83D\uDD1E \n" +
                "1 dag actief op Marktplaats \n");
    }

    @Test
    public void spamMessage3ContainsUrl() {
        assertContainsUrl("Ben je het beu om een hete meid te zoeken? Wil je waarschijnlijk vanavond seks? Uw problemen worden hier opgelost: Веn jе het beu om ееn hеtе meid tе zоеkеn? Wil jе waаrsсhijnlijk vаnavоndsеks? Uw prоblemen wоrden hiеr орgelоst: http://7sex.nl Slесhts 18+");
    }

    private static void assertContainsUrl(String text) {
        BlockUrlMessageBeforeSellerRepliesFilter filter = new BlockUrlMessageBeforeSellerRepliesFilter();


        Map<String, String> metadata = new HashMap<>();
        metadata.put("X-User-Message", text);

        Message message = mock(Message.class);
        when(message.getCaseInsensitiveHeaders()).thenReturn(metadata);

        assertThat(filter.containsUrls(message), is(true));
    }

    private static Message mockMessage(MessageDirection direction) {
        Message msg = mock(Message.class);
        when(msg.getMessageDirection()).thenReturn(direction);
        return msg;
    }
}