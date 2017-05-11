package com.ecg.gumtree.comaas.filter.volume;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.Result;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.config.VelocityFilterConfig;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static com.ecg.gumtree.comaas.filter.volume.GumtreeVolumeFilter.MARKED_SEEN_BY_VOLUME_FILTER;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GumtreeVolumeFilterTest.TestContext.class)
public class GumtreeVolumeFilterTest {
    @Autowired
    private GumtreeVolumeFilter filter;

    @MockBean
    private SearchService searchService;

    @MockBean
    private EventStreamProcessor eventStreamProcessor;

    @Mock
    private VolumeFilterServiceHelper volumeFilterServiceHelper;

    @Autowired
    private SharedBrain sharedBrain;

    private Mail mail = mock(Mail.class);

    @Before
    public void setup() {
        filter = filter
                .withVolumeFilterServiceHelper(volumeFilterServiceHelper)
                .withSearchService(searchService)
                .withEventStreamProcessor(eventStreamProcessor);
    }

    @Test
    public void messageInExcludedCategoryDoesNotGetProcessedByVelocityFilter() {
        MutableConversation conversation = createDefaultMutableConversation();
        createMessageForConversation(conversation, "messageId1");
        MessageProcessingContext messageProcessingContext = createMessageProcessingContext(conversation, "messageId1", ImmutableSet.of(1234L));

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertThat(feedbacks.size()).isEqualTo(0);
    }

    @Test
    public void secondMessageToSameAdDoesNotGetProcessedByVelocityFilter() {
        MutableConversation conversation = createDefaultMutableConversation();
        createMessageForConversation(conversation, "messageId1");
        createMessageForConversation(conversation, "messageId2");
        MessageProcessingContext messageProcessingContext = createMessageProcessingContext(conversation, "messageId2");

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertThat(feedbacks.size()).isEqualTo(0);
    }

    @Test
    public void temporarilyWhitelistedUserDoesNotGetProcessedByVelocityFilter() {
        MutableConversation conversation = createDefaultMutableConversation();
        createMessageForConversation(conversation, "messageId1");
        MessageProcessingContext messageProcessingContext = createMessageProcessingContext(conversation, "messageId1");

        RtsSearchResponse stubbedResponse = new RtsSearchResponse(ImmutableList.of(), 0, 2, 2);
        when(searchService.search(any(SearchMessagePayload.class))).thenReturn(stubbedResponse);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertThat(feedbacks.size()).isEqualTo(0);
    }

    @Test
    public void velocityFilterAllowsInfrequentMessageToPass() {
        MutableConversation conversation = createDefaultMutableConversation();
        createMessageForConversation(conversation, "messageId1");
        MessageProcessingContext messageProcessingContext = createMessageProcessingContext(conversation, "messageId1");

        RtsSearchResponse stubbedResponse = new RtsSearchResponse(ImmutableList.of(), 0, 0, 0);
        when(searchService.search(any(SearchMessagePayload.class))).thenReturn(stubbedResponse);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertThat(feedbacks.size()).isEqualTo(0);
    }

    @Test
    public void velocityFilterExceededHoldsMessageToPass() throws Exception {
        MutableConversation conversation = createDefaultMutableConversation();
        createMessageForConversation(conversation, "messageId1");
        MessageProcessingContext messageProcessingContext = createMessageProcessingContext(conversation, "messageId1");

        RtsSearchResponse stubbedResponse = new RtsSearchResponse(ImmutableList.of(), 0, 0, 0);
        when(searchService.search(any(SearchMessagePayload.class))).thenReturn(stubbedResponse);
        when(eventStreamProcessor.count(anyString(), anyString())).thenReturn(3L);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertThat(feedbacks.size()).isEqualTo(1);
        FilterFeedback actual = feedbacks.get(0);
        assertThat(actual.getResultState()).isEqualTo(FilterResultState.HELD);

        JsonNode jsonNode = new ObjectMapper().readTree(actual.getDescription());
        assertThat(jsonNode.get("description").textValue()).isEqualTo("More than 2 messages in 100 seconds");
        assertThat(actual.getScore()).isEqualTo(0);
        assertThat(actual.getUiHint()).isEqualTo("buyer@rts.com");
    }

    @Test
    public void velocityFilterMarksSeenWhenNotYetMarked() throws Exception {
        Map<String, Object> filterContext = new HashMap<>();
        markSeenSetup(filterContext);
        verify(sharedBrain, times(1)).markSeen(anyString(), anyString(), anyString());
        assertThat(filterContext.get(MARKED_SEEN_BY_VOLUME_FILTER)).isEqualTo(true);
    }

    @Test
    public void velocityFilterDoesNotMarkSeenOnlyWhenAlreadyMarked() throws Exception {
        Map<String, Object> filterContext = new HashMap<>();
        filterContext.put(MARKED_SEEN_BY_VOLUME_FILTER, true);

        markSeenSetup(filterContext);
        verify(sharedBrain, never()).markSeen(anyString(), anyString(), anyString());
        assertThat(filterContext.get(MARKED_SEEN_BY_VOLUME_FILTER)).isEqualTo(true);
    }

    private void markSeenSetup(Map<String, Object> filterContext) {
        MessageProcessingContext messageProcessingContext = mock(MessageProcessingContext.class);
        when(messageProcessingContext.getFilterContext()).thenReturn(filterContext);
        Message message1 = mock(Message.class);
        when(message1.getId()).thenReturn("a");
        when(messageProcessingContext.getMessage()).thenReturn(message1);
        Conversation conversation = mock(Conversation.class);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message1));
        when(messageProcessingContext.getConversation()).thenReturn(conversation);

        RtsSearchResponse stubbedResponse = new RtsSearchResponse(ImmutableList.of(), 0, 0, 0);
        when(searchService.search(any(SearchMessagePayload.class))).thenReturn(stubbedResponse);

        filter.filter(messageProcessingContext);
    }

    private void createMessageForConversation(MutableConversation conversation, String messageId) {
        AddMessageCommand command = AddMessageCommandBuilder.anAddMessageCommand(conversation.getId(), messageId)
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withReceivedAt(new DateTime())
                .build();
        conversation.applyCommand(command);
    }

    private MessageProcessingContext createMessageProcessingContext(MutableConversation conversation, String messageId) {
        return createMessageProcessingContext(conversation, messageId, Collections.emptySet());
    }

    private MessageProcessingContext createMessageProcessingContext(MutableConversation conversation, String messageId, Set<Long> categoryBreadCrumb) {
        MessageProcessingContext context = new MessageProcessingContext(mail, messageId,
                new ProcessingTimeGuard(1L));

        context.setConversation(conversation);
        context.getFilterContext().put("categoryBreadCrumb", categoryBreadCrumb);

        return context;
    }

    private MutableConversation createDefaultMutableConversation() {
        NewConversationCommand command = new NewConversationCommand("conversation1", "adId1",
                "buyer@rts.com", "sellerId", "buyerSecret", "sellerSecret", new DateTime(),
                ConversationState.ACTIVE, ImmutableMap.of());
        return DefaultMutableConversation.create(command);

    }

    @Configuration
    static class TestContext {
        @MockBean
        private SharedBrain sharedBrain;

        @Bean
        public VelocityFilterConfig filterConfig() throws Exception {
            return new VelocityFilterConfig.Builder(State.ENABLED, 1, Result.HOLD)
                    .withMessageState(VelocityFilterConfig.MessageState.CREATED)
                    .withFilterField(VelocityFilterConfig.FilterField.EMAIL)
                    .withExceeding(true)
                    .withExemptedCategories(ImmutableList.of(1234L, 4321L))
                    .withMessages(2)
                    .withSeconds(100)
                    .withWhitelistSeconds(3600)
                    .withVersion("1.0.0")
                    .build();
        }

        @Bean
        public GumtreeVolumeFilter filter(VelocityFilterConfig filterConfig) {
            return new GumtreeVolumeFilter()
                    .withPluginConfig(mock(Filter.class))
                    .withFilterConfig(filterConfig)
                    .withSharedBrain(sharedBrain);
        }
    }
}
