package com.ecg.comaas.mp.filter.volume;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.comaas.mp.filter.volume.VolumeFilterConfiguration.VolumeRule;
import com.ecg.comaas.mp.filter.volume.persistence.CassandraVolumeFilterEventRepository;
import com.ecg.replyts.app.Mails;
import com.ecg.replyts.app.filterchain.FilterChain;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder;
import com.ecg.replyts.core.api.model.conversation.command.MessageTerminatedCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CassandraVolumeFilterIntegrationTest {
    private CassandraIntegrationTestProvisioner casdb = CassandraIntegrationTestProvisioner.getInstance();

    private Session session;

    private CassandraVolumeFilterEventRepository repository;
    private VolumeFilterConfiguration config;

    private String keyspaceName = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName("v_");

    @Before
    public void init() {
        session = casdb.loadSchema(keyspaceName, "cassandra_schema.cql", "cassandra_volume_filter_schema.cql");
        config = new VolumeFilterConfiguration(newArrayList(new VolumeRule(1L, HOURS, 1L, 100)));
        repository = new CassandraVolumeFilterEventRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
    }

    @After
    public void cleanup() {
        casdb.cleanTables(session, keyspaceName);
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldTriggerWhenVolumeRuleIsViolated() throws Exception {
        VolumeFilter filter = new VolumeFilter(config, repository);

        // conversation A with 1 message, no violation
        byte[] mail = "From: buyer@example.com\nDelivered-To: bar\nMessage-ID: 333\n\nhello".getBytes();
        MessageProcessingContext context = new MessageProcessingContext(Mails.readMail(mail), "333", new ProcessingTimeGuard(300L));
        MutableConversation conversation = DefaultMutableConversation.create(new NewConversationCommand(
                "1", "1", "buyer@example.com", "seller@example.com", "abc123", "xyz809", new DateTime(), ConversationState.ACTIVE, new HashMap()));
        AddMessageCommand addMessageCommand = AddMessageCommandBuilder.anAddMessageCommand("1", "333").
                withMessageDirection(MessageDirection.BUYER_TO_SELLER).
                withReceivedAt(new DateTime()).
                addHeader("To", "seller@example.com").
                addHeader("From", "buyer@example.com").
                build();
        conversation.applyCommand(addMessageCommand);
        context.setConversation(conversation);

        List<FilterFeedback> feedback = filter.filter(context);
        assertThat(feedback.size(), is(0));

        // conversation B with 1 message, violates the volume rule since it's the second message from the buyer in an hour
        mail = "From: buyer@example.com\nDelivered-To: bar2\nMessage-ID: 334\n\nhello".getBytes();
        context = new MessageProcessingContext(Mails.readMail(mail), "334", new ProcessingTimeGuard(300L));
        conversation = DefaultMutableConversation.create(new NewConversationCommand(
                "2", "2", "buyer@example.com", "seller2@example.com", "abc123", "xyz809", new DateTime(), ConversationState.ACTIVE, new HashMap()));
        addMessageCommand = AddMessageCommandBuilder.anAddMessageCommand("2", "334").
                withMessageDirection(MessageDirection.BUYER_TO_SELLER).
                withReceivedAt(new DateTime()).
                addHeader("To", "seller2@example.com").
                addHeader("From", "buyer@example.com").
                build();
        conversation.applyCommand(addMessageCommand);
        context.setConversation(conversation);

        feedback = filter.filter(context);
        assertThat(feedback.get(0).getScore(), is(100));
    }

    @Test
    public void shouldFilterIfPreviousMessageWasBlocked() throws Exception {
        VolumeFilter filter = new VolumeFilter(config, repository);

        // conversation A with 1 message, no violation
        byte[] mail = "From: buyer@example.com\nDelivered-To: bar\nMessage-ID: 333\n\nhello".getBytes();
        MessageProcessingContext context = new MessageProcessingContext(Mails.readMail(mail), "333", new ProcessingTimeGuard(300L));
        MutableConversation conversation = DefaultMutableConversation.create(new NewConversationCommand(
                "1", "1", "buyer@example.com", "seller@example.com", "abc123", "xyz809", new DateTime(), ConversationState.ACTIVE, new HashMap()));
        AddMessageCommand addMessageCommand = AddMessageCommandBuilder.anAddMessageCommand("1", "333").
                withMessageDirection(MessageDirection.BUYER_TO_SELLER).
                withReceivedAt(new DateTime()).
                addHeader("To", "seller@example.com").
                addHeader("From", "buyer@example.com").
                build();
        conversation.applyCommand(addMessageCommand);
        context.setConversation(conversation);

        List<FilterFeedback> feedback = filter.filter(context);
        assertThat(feedback.size(), is(0));

        // conversation B with 1 message, violates the volume rule
        mail = "From: buyer@example.com\nDelivered-To: bar2\nMessage-ID: 334\n\nhello".getBytes();
        context = new MessageProcessingContext(Mails.readMail(mail), "334", new ProcessingTimeGuard(300L));
        conversation = DefaultMutableConversation.create(new NewConversationCommand(
                "2", "2", "buyer@example.com", "seller2@example.com", "abc123", "xyz809", new DateTime(), ConversationState.ACTIVE, new HashMap()));
        addMessageCommand = AddMessageCommandBuilder.anAddMessageCommand("2", "334").
                withMessageDirection(MessageDirection.BUYER_TO_SELLER).
                withReceivedAt(new DateTime()).
                addHeader("To", "seller2@example.com").
                addHeader("From", "buyer@example.com").
                build();
        conversation.applyCommand(addMessageCommand);
        context.setConversation(conversation);

        feedback = filter.filter(context);
        assertThat(feedback.get(0).getScore(), is(100));

        // mimic filterchain blocking the message
        conversation.applyCommand(new MessageTerminatedCommand("2", "334", FilterChain.class, "FilterChain ended up in result state DROPPED", MessageState.BLOCKED));

        // conversation B with a 2nd message, is processed by the filter since the previous message was BLOCKED. Violates the volume rule
        mail = "From: buyer@example.com\nDelivered-To: bar2\nMessage-ID: 335\n\nhello".getBytes();
        context = new MessageProcessingContext(Mails.readMail(mail), "335", new ProcessingTimeGuard(300L));
        addMessageCommand = AddMessageCommandBuilder.anAddMessageCommand("2", "335").
                withMessageDirection(MessageDirection.BUYER_TO_SELLER).
                withReceivedAt(new DateTime()).
                addHeader("To", "seller2@example.com").
                addHeader("From", "buyer@example.com").
                build();
        conversation.applyCommand(addMessageCommand);
        context.setConversation(conversation);

        List<FilterFeedback> feedback2 = filter.filter(context);
        assertThat(feedback2.get(0).getScore(), is(100));
    }

}
