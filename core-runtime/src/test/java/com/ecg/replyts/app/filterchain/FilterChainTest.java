package com.ecg.replyts.app.filterchain;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspector;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.configadmin.ClusterRefreshPublisher;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationAdmin;
import com.ecg.replyts.core.runtime.configadmin.PluginInstanceReference;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.ecg.replyts.core.api.model.conversation.FilterResultState.DROPPED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FilterChainTest.TestContext.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import({ FilterChain.class, FilterListProcessor.class })
public class FilterChainTest {
    private static final String CONVERSATION_ID = "MY_CONVERSATION";
    private static final String MESSAGE_ID = "MY_MESSAGE";

    @MockBean
    private MutableConversationRepository mutableConversationRepository;

    @MockBean
    private MutableMail mail;

    @MockBean
    private Message message;

    @MockBean
    private PluginConfiguration pluginConfiguration;

    @MockBean
    private HeldMailRepository heldMailRepository;

    @MockBean
    private FilterListMetrics metrics;

    @Autowired
    private ConfigurationAdmin<Filter> filterConfig;

    @Autowired
    private ConfigurationAdmin<ResultInspector> inspectorConfig;

    @Autowired
    private PluginInstanceReference<Filter> filterRef;

    @Autowired
    private FilterChain chain;

    private TestMessageProcessingContext testMessageProcessingContext;

    public static class ExampleFilterFactory implements BasePluginFactory<Object> {
        @Override
        public Object createPlugin(String instanceName, JsonNode configuration) {
            throw new UnsupportedOperationException();
        }
    }

    @Before
    public void setup() {
        MutableConversation conversation = setUpConversation();

        when(mail.makeMutableCopy()).thenReturn(mail);
        when(mail.getMessageId()).thenReturn(MESSAGE_ID);

        when(filterRef.getConfiguration()).thenReturn(pluginConfiguration);
        when(pluginConfiguration.getId()).thenReturn(new ConfigurationId(ExampleFilterFactory.class, "instance"));

        testMessageProcessingContext = new TestMessageProcessingContext(mail);
        testMessageProcessingContext.setConversation(conversation);
    }

    private static MutableConversation setUpConversation() {
        NewConversationCommand newConversationCommand =
                NewConversationCommandBuilder.aNewConversationCommand("4321").
                        withAdId(CONVERSATION_ID).
                        withBuyer("john@ymail.com", "ss983e9s0f7ds").
                        withSeller("mary@gmail.com", "x6cvm8dp9y3k9").
                        withCreatedAt(new DateTime(2012, 1, 10, 9, 11, 43)).
                        withState(ConversationState.ACTIVE).
                        build();
        AddMessageCommand messageCommand = AddMessageCommandBuilder.anAddMessageCommand(CONVERSATION_ID, MESSAGE_ID)
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withReceivedAt(new DateTime())
                .withHeaders(new HashMap<>())
                .build();
        DefaultMutableConversation conversation = DefaultMutableConversation.create(newConversationCommand);
        conversation.applyCommand(messageCommand);
        return conversation;
    }

    @Test
    public void disabledFiltersAreSkipped() {
        Filter mockFilter = mock(Filter.class);
        when(filterRef.getState()).thenReturn(PluginState.DISABLED);
        when(filterRef.getCreatedService()).thenReturn(mockFilter);
        List<PluginInstanceReference<Filter>> filterList = new ArrayList<>();
        filterList.add(filterRef);
        when(filterConfig.getRunningServices()).thenReturn(filterList);

        chain.filter(testMessageProcessingContext);

        verify(filterRef, never()).getCreatedService();
    }

    @Test
    public void evaluationFiltersAreApplied() {
        Filter mockFilter = mock(Filter.class);
        when(filterRef.getState()).thenReturn(PluginState.EVALUATION);
        when(filterRef.getCreatedService()).thenReturn(mockFilter);
        List<PluginInstanceReference<Filter>> filterList = new ArrayList<>();
        filterList.add(filterRef);
        when(filterConfig.getRunningServices()).thenReturn(filterList);

        chain.filter(testMessageProcessingContext);

        verify(mockFilter, times(1)).filter(any(MessageProcessingContext.class));
    }

    @Test
    public void enabledFiltersAreApplied() {
        List<FilterFeedback> processingFeedbacks = singleFeedbackWithState(FilterResultState.OK);
        Filter mockFilter = mock(Filter.class);
        when(mockFilter.filter(any(MessageProcessingContext.class))).thenReturn(processingFeedbacks);

        when(filterRef.getState()).thenReturn(PluginState.ENABLED);
        when(filterRef.getCreatedService()).thenReturn(mockFilter);

        List<PluginInstanceReference<Filter>> filterList = new ArrayList<>();
        filterList.add(filterRef);
        when(filterConfig.getRunningServices()).thenReturn(filterList);

        chain.filter(testMessageProcessingContext);

        verify(mockFilter).filter(any(MessageProcessingContext.class));
    }

    @Test
    public void contextTerminateIsCalledWhenAppropriateForHeldState() {
        List<FilterFeedback> processingFeedbacks = singleFeedbackWithState(FilterResultState.HELD);
        Filter mockFilter = mock(Filter.class);
        when(mockFilter.filter(any(MessageProcessingContext.class))).thenReturn(processingFeedbacks);

        when(filterRef.getState()).thenReturn(PluginState.ENABLED);
        when(filterRef.getCreatedService()).thenReturn(mockFilter);
        List<PluginInstanceReference<Filter>> filterList = new ArrayList<>();
        filterList.add(filterRef);
        when(filterConfig.getRunningServices()).thenReturn(filterList);

        chain.filter(testMessageProcessingContext);

        int terminateProcessingCounter = testMessageProcessingContext.getTerminateProcessingCounter();
        assertThat(terminateProcessingCounter, is(1));
    }

    @Test
    public void contextTerminateIsCalledWhenAppropriateForDroppedState() {
        List<FilterFeedback> processingFeedbacks = singleFeedbackWithState(DROPPED);
        Filter mockFilter = mock(Filter.class);
        when(mockFilter.filter(any(MessageProcessingContext.class))).thenReturn(processingFeedbacks);

        when(filterRef.getState()).thenReturn(PluginState.ENABLED);
        when(filterRef.getCreatedService()).thenReturn(mockFilter);

        List<PluginInstanceReference<Filter>> filterList = new ArrayList<>();
        filterList.add(filterRef);
        when(filterConfig.getRunningServices()).thenReturn(filterList);

        chain.filter(testMessageProcessingContext);
        int terminateProcessingCounter = testMessageProcessingContext.getTerminateProcessingCounter();
        assertThat(terminateProcessingCounter, is(1));
    }

    @Test
    public void contextTerminatedIsSkippedWhenNotApplicable() {
        List<FilterFeedback> processingFeedbacks = singleFeedbackWithState(FilterResultState.OK);
        Filter mockFilter = mock(Filter.class);
        when(mockFilter.filter(any(MessageProcessingContext.class))).thenReturn(processingFeedbacks);

        when(filterRef.getState()).thenReturn(PluginState.ENABLED);
        when(filterRef.getCreatedService()).thenReturn(mockFilter);

        List<PluginInstanceReference<Filter>> filterList = new ArrayList<>();
        filterList.add(filterRef);
        when(filterConfig.getRunningServices()).thenReturn(filterList);

        chain.filter(testMessageProcessingContext);
        int terminateProcessingCounter = testMessageProcessingContext.getTerminateProcessingCounter();
        assertThat(terminateProcessingCounter, is(0));
    }

    @Test
    public void resultInspectorIsCalledForProcessingFeedback() {
        PluginInstanceReference<ResultInspector> resultInspectorReference = mock(PluginInstanceReference.class);

        List<PluginInstanceReference<ResultInspector>> pluginInstanceReferences = new ArrayList<>();
        pluginInstanceReferences.add(resultInspectorReference);
        when(inspectorConfig.getRunningServices()).thenReturn(pluginInstanceReferences);
        ResultInspector mockResultInspector = mock(ResultInspector.class);
        when(resultInspectorReference.getCreatedService()).thenReturn(mockResultInspector);

        List<FilterFeedback> processingFeedbacks = singleFeedbackWithState(DROPPED);
        Filter mockFilter = mock(Filter.class);
        when(mockFilter.filter(any(MessageProcessingContext.class))).thenReturn(processingFeedbacks);

        PluginInstanceReference<Filter> filterRef = mock(PluginInstanceReference.class);
        when(filterRef.getConfiguration()).thenReturn(pluginConfiguration);
        when(filterRef.getState()).thenReturn(PluginState.ENABLED);
        when(filterRef.getCreatedService()).thenReturn(mockFilter);
        List<PluginInstanceReference<Filter>> filterList = new ArrayList<>();
        filterList.add(filterRef);
        when(filterConfig.getRunningServices()).thenReturn(filterList);

        chain.filter(testMessageProcessingContext);
        verify(mockResultInspector).inspect(any(List.class));
    }

    @Test
    public void overallFilterResultStateOkDoesNotOverrideOthers() {

        List<FilterFeedback> processingFeedbacks1 = singleFeedbackWithState(DROPPED);
        Filter mockFilter1 = mock(Filter.class);
        when(mockFilter1.filter(any(MessageProcessingContext.class))).thenReturn(processingFeedbacks1);

        List<FilterFeedback> processingFeedbacks2 = singleFeedbackWithState(FilterResultState.OK);
        Filter mockFilter2 = mock(Filter.class);
        when(mockFilter2.filter(any(MessageProcessingContext.class))).thenReturn(processingFeedbacks2);

        PluginInstanceReference<Filter> filterRef1 = mock(PluginInstanceReference.class);
        when(filterRef1.getConfiguration()).thenReturn(pluginConfiguration);
        when(filterRef1.getState()).thenReturn(PluginState.ENABLED);
        when(filterRef1.getCreatedService()).thenReturn(mockFilter1);
        PluginInstanceReference<Filter> filterRef2 = mock(PluginInstanceReference.class);
        when(filterRef2.getConfiguration()).thenReturn(pluginConfiguration);
        when(filterRef2.getState()).thenReturn(PluginState.ENABLED);
        when(filterRef2.getCreatedService()).thenReturn(mockFilter2);

        List<PluginInstanceReference<Filter>> filterList = new ArrayList<PluginInstanceReference<Filter>>();
        filterList.add(filterRef1);
        filterList.add(filterRef2);
        when(filterConfig.getRunningServices()).thenReturn(filterList);

        chain.filter(testMessageProcessingContext);

        Message message = testMessageProcessingContext.getMessage();
        assertThat(message.getFilterResultState(), is(DROPPED));
    }

    @Test
    public void messageReflectsFilterProcessingFeedback() {
        List<FilterFeedback> processingFeedbacks = ImmutableList.of(
                new FilterFeedback("Test caught", "This is a test filter",
                        10, FilterResultState.HELD));

        Filter mockFilter = mock(Filter.class);
        when(mockFilter.filter(any(MessageProcessingContext.class))).thenReturn(processingFeedbacks);

        PluginInstanceReference<Filter> filterRef = mock(PluginInstanceReference.class);
        when(filterRef.getConfiguration()).thenReturn(pluginConfiguration);
        when(filterRef.getState()).thenReturn(PluginState.ENABLED);
        when(filterRef.getCreatedService()).thenReturn(mockFilter);

        List<PluginInstanceReference<Filter>> filterList = new ArrayList<>();
        filterList.add(filterRef);
        when(filterConfig.getRunningServices()).thenReturn(filterList);

        chain.filter(testMessageProcessingContext);

        List<ProcessingFeedback> filteredProcessingFeedbacks =
                testMessageProcessingContext.getMessage().getProcessingFeedback();
        assertThat(filteredProcessingFeedbacks.size(), is(1));

        ProcessingFeedback feedback = filteredProcessingFeedbacks.iterator().next();
        assertThat(feedback.getFilterName(), equalTo(ExampleFilterFactory.class.getName()));
    }

    private ImmutableList<FilterFeedback> singleFeedbackWithState(FilterResultState filterResultState) {
        return ImmutableList.of(new FilterFeedback("u", "d", 0, filterResultState));
    }

    private static class TestMessageProcessingContext extends MessageProcessingContext {
        private int terminateProcessingCounter;

        private TestMessageProcessingContext(Mail mail) {
            super(mail, MESSAGE_ID, new ProcessingTimeGuard(100L));
        }

        @Override
        public Conversation getConversation() {
            return super.getConversation();
        }

        @Override
        public void terminateProcessing(MessageState state, Object issuer, String reason) {
            super.terminateProcessing(state, issuer, reason);
            terminateProcessingCounter++;
        }

        public int getTerminateProcessingCounter() {
            return terminateProcessingCounter;
        }
    }

    @Configuration
    static class TestContext {
        @MockBean
        private ClusterRefreshPublisher clusterRefreshPublisher;

        @Bean
        public ConfigurationAdmin<Filter> filterConfigurationAdmin() {
            return mock(ConfigurationAdmin.class);
        }

        @Bean
        public ConfigurationAdmin<ResultInspector> resultInspectorConfigurationAdmin() {
            return mock(ConfigurationAdmin.class);
        }

        @Bean
        public PluginInstanceReference<Filter> filterRef() {
            return mock(PluginInstanceReference.class);
        }
    }
}