package com.ecg.messagecenter.ebayk.webapi;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.ecg.messagecenter.core.persistence.simple.CassandraSimplePostBoxConfiguration;
import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.ebayk.persistence.ConversationThread;
import com.ecg.messagecenter.ebayk.webapi.responses.PostBoxListItemResponse;
import com.ecg.replyts.app.preprocessorchain.preprocessors.ConversationResumer;
import com.ecg.replyts.app.preprocessorchain.preprocessors.IdBasedConversationResumer;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierConfiguration;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultCassandraConversationRepository;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.ecg.sync.PostBoxResponse;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ecg.messagecenter.ebayk.webapi.PostBoxResponseAssertions.assertConversationsOrder;
import static com.ecg.messagecenter.ebayk.webapi.PostBoxResponseAssertions.assertConversationsOriginalOrder;
import static com.ecg.messagecenter.ebayk.webapi.PostBoxResponseAssertions.assertConversationsSize;
import static com.ecg.messagecenter.ebayk.webapi.PostBoxResponseAssertions.assertDetailConversation;
import static com.ecg.messagecenter.ebayk.webapi.PostBoxResponseAssertions.assertProcessingStatusOk;
import static com.ecg.messagecenter.ebayk.webapi.PostBoxResponseAssertions.assertUnreadMessages;
import static org.joda.time.DateTime.now;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {PostBoxOverviewControllerIntegrationTest.TestContext.class})
@TestPropertySource(properties = {
  "persistence.strategy = cassandra",
  "persistence.cassandra.conversation.class = com.ecg.messagecenter.ebayk.persistence.ConversationThread"
})
public class PostBoxOverviewControllerIntegrationTest {
    static final String DETAILED_CONVERSATION = "detailed-conversation";
    static final List<String> DETAILED_CONVERSATION_LIST = Collections.singletonList(DETAILED_CONVERSATION);

    private static final Integer[] FIRST_PAGE = {0, 1, 2, 3};
    private static final Integer[] SECOND_PAGE = {4, 5, 6, 7};
    private static final Integer[] THIRD_PAGE = {8, 9};
    private static final Integer[] FORTH_PAGE = {};

    static final String BUYER_EMAIL = "buyer@email.com";
    static final String SELLER_EMAIL = "seller@email.com";
    static final Long BUYER_ID = 1L;
    static final Long SELLER_ID = 2L;
    static final String BUYER_NAME = "buyer-name";
    static final String SELLER_NAME = "seller-name";
    static final String AD_ID = "123";

    static final DateTime NOW = now();

    @Autowired
    private SimplePostBoxRepository postBoxRepository;

    @Autowired
    private PostBoxOverviewController controller;

    private PostBox<AbstractConversationThread> postBox;

    /**
     * Creates a new PostBox and creates conversations.
     * - conversation has ID: conversation-(number)
     * - direction: even - buyer, odd - seller
     * - order of the messages also determines how many days ago was messages created/modified/received
     * - creates 5 unread messages in conversation
     */
    @Before
    public void setUp() {
        List<AbstractConversationThread> conversations = IntStream.rangeClosed(0, 9)
                .mapToObj(this::createConversation)
                .collect(Collectors.toList());

        postBox = new PostBox<>(BUYER_EMAIL, Optional.empty(), conversations);
        postBoxRepository.write(postBox);
        postBoxRepository.upsertThread(postBox.getId(), conversations.get(1), true);
        postBoxRepository.upsertThread(postBox.getId(), conversations.get(1), true);
        postBoxRepository.upsertThread(postBox.getId(), conversations.get(3), true);
        postBoxRepository.upsertThread(postBox.getId(), conversations.get(6), true);
        postBoxRepository.upsertThread(postBox.getId(), conversations.get(7), true);
    }

    /**
     * Deletes all conversations from PostBox.
     */
    @After
    public void tearDown() {
        List<String> conversationIds = postBox.getConversationThreads().stream()
                .map(AbstractConversationThread::getConversationId)
                .peek(postBox::removeConversation)
                .collect(Collectors.toList());

        postBoxRepository.deleteConversations(postBox, conversationIds);
    }

    @Test
    public void testSimplePut() {
        ResponseObject<PostBoxResponse> response = controller.readPostBoxByEmail(BUYER_EMAIL, 50, 0);

        assertProcessingStatusOk(response);
        assertUnreadMessages(response, 0);
        assertConversationsSize(response, 10);
        assertConversationsOriginalOrder(response);
    }

    @Test
    public void testSimpleGet() {
        ResponseObject<PostBoxResponse> response = controller.getPostBoxByEmail(BUYER_EMAIL, 50, 0);

        assertProcessingStatusOk(response);
        assertUnreadMessages(response, 5);
        assertConversationsSize(response, 10);
        assertConversationsOriginalOrder(response);
    }

    @Test
    public void testGetPaging() {
        ResponseObject<PostBoxResponse> response = controller.getPostBoxByEmail(BUYER_EMAIL, 4, 0);

        assertProcessingStatusOk(response);
        assertUnreadMessages(response, 5);
        assertConversationsSize(response, 4);
        assertConversationsOrder(response, FIRST_PAGE);

        ResponseObject<PostBoxResponse> response2 = controller.getPostBoxByEmail(BUYER_EMAIL, 4, 1);

        assertProcessingStatusOk(response2);
        assertUnreadMessages(response2, 5);
        assertConversationsSize(response2, 4);
        assertConversationsOrder(response2, SECOND_PAGE);

        ResponseObject<PostBoxResponse> response3 = controller.getPostBoxByEmail(BUYER_EMAIL, 4, 2);

        assertProcessingStatusOk(response3);
        assertUnreadMessages(response3, 5);
        assertConversationsSize(response3, 2);
        assertConversationsOrder(response3, THIRD_PAGE);

        ResponseObject<PostBoxResponse> response4 = controller.getPostBoxByEmail(BUYER_EMAIL, 4, 3);

        // Out ouf conversations - blank page
        assertProcessingStatusOk(response4);
        assertUnreadMessages(response4, 5);
        assertConversationsSize(response4, 0);
        assertConversationsOrder(response4, FORTH_PAGE);
    }

    @Test
    public void testReadPaging() {
        ResponseObject<PostBoxResponse> response = controller.readPostBoxByEmail(BUYER_EMAIL, 4, 0);

        // 3 messages should be read - 2x1, 3
        assertProcessingStatusOk(response);
        assertUnreadMessages(response, 2);
        assertConversationsSize(response, 4);
        assertConversationsOrder(response, FIRST_PAGE);

        ResponseObject<PostBoxResponse> response2 = controller.readPostBoxByEmail(BUYER_EMAIL, 4, 1);

        // 2 messages should be read - 6, 7
        assertProcessingStatusOk(response2);
        assertUnreadMessages(response2, 0);
        assertConversationsSize(response2, 4);
        assertConversationsOrder(response2, SECOND_PAGE);

        ResponseObject<PostBoxResponse> response3 = controller.readPostBoxByEmail(BUYER_EMAIL, 4, 2);

        // 0 messages should be read
        assertProcessingStatusOk(response3);
        assertUnreadMessages(response3, 0);
        assertConversationsSize(response3, 2);
        assertConversationsOrder(response3, THIRD_PAGE);

        ResponseObject<PostBoxResponse> response4 = controller.readPostBoxByEmail(BUYER_EMAIL, 4, 3);

        // No remaining conversations - blank page
        assertProcessingStatusOk(response4);
        assertUnreadMessages(response4, 0);
        assertConversationsSize(response4, 0);
        assertConversationsOrder(response4);
    }

    /**
     * Read only the second page and ensure that the messages on the first page was not marked as read.
     */
    @Test
    public void testReadCertainPage() {
        ResponseObject<PostBoxResponse> response = controller.readPostBoxByEmail(BUYER_EMAIL, 4, 1);

        /*
         * Check that the state of the postbox after the reading the second page.
         * - 3 unread messages should be available on the first page
         */
        assertProcessingStatusOk(response);
        assertUnreadMessages(response, 3);
        assertConversationsSize(response, 4);
        assertConversationsOrder(response, SECOND_PAGE);

        ResponseObject<PostBoxResponse> response2 = controller.getPostBoxByEmail(BUYER_EMAIL, 4, 0);

        /*
         * Check that the first page is in the same state as before
         */
        assertProcessingStatusOk(response2);
        assertUnreadMessages(response2, 3);
        assertConversationsSize(response2, 4);
        assertConversationsOrder(response2, FIRST_PAGE);
    }

    @Test
    public void testConversationDetailBuyerOutbound() {
        postBoxRepository.upsertThread(postBox.getId(), createConversation(DETAILED_CONVERSATION, MessageDirection.BUYER_TO_SELLER), false);

        PostBoxListItemResponse conversation = getDetailedConversation(BUYER_EMAIL);
        assertDetailConversation(conversation, BUYER_EMAIL, ConversationRole.Buyer, MailTypeRts.OUTBOUND);
        postBoxRepository.deleteConversations(postBox, DETAILED_CONVERSATION_LIST);
    }

    @Test
    public void testConversationDetailBuyerInbound() {
        postBoxRepository.upsertThread(postBox.getId(), createConversation(DETAILED_CONVERSATION, MessageDirection.SELLER_TO_BUYER), false);

        PostBoxListItemResponse conversation = getDetailedConversation(BUYER_EMAIL);
        assertDetailConversation(conversation, BUYER_EMAIL, ConversationRole.Buyer, MailTypeRts.INBOUND);
        postBoxRepository.deleteConversations(postBox, DETAILED_CONVERSATION_LIST);
    }

    @Test
    public void testConversationDetailSellerOutbound() {
        List<AbstractConversationThread> conversationToSave =
                Collections.singletonList(createConversation(DETAILED_CONVERSATION, MessageDirection.SELLER_TO_BUYER));

        PostBox<AbstractConversationThread> postBox = new PostBox<>(SELLER_EMAIL, Optional.empty(), conversationToSave);
        postBoxRepository.write(postBox);

        PostBoxListItemResponse conversation = getDetailedConversation(SELLER_EMAIL);
        assertDetailConversation(conversation, SELLER_EMAIL, ConversationRole.Seller, MailTypeRts.OUTBOUND);
        postBoxRepository.deleteConversations(postBox, DETAILED_CONVERSATION_LIST);
    }

    @Test
    public void testConversationDetailSellerInbound() {
        List<AbstractConversationThread> conversationToSave =
                Collections.singletonList(createConversation(DETAILED_CONVERSATION, MessageDirection.BUYER_TO_SELLER));

        PostBox<AbstractConversationThread> postBox = new PostBox<>(SELLER_EMAIL, Optional.empty(), conversationToSave);
        postBoxRepository.write(postBox);

        PostBoxListItemResponse conversation = getDetailedConversation(SELLER_EMAIL);
        assertDetailConversation(conversation, SELLER_EMAIL, ConversationRole.Seller, MailTypeRts.INBOUND);
        postBoxRepository.deleteConversations(postBox, DETAILED_CONVERSATION_LIST);
    }

    private PostBoxListItemResponse getDetailedConversation(String email) {
        List<PostBoxListItemResponse> conversations = controller.getPostBoxByEmail(email, 50, 0).getBody().getConversations();
        return conversations.stream().filter(item -> DETAILED_CONVERSATION.equalsIgnoreCase(item.getId())).findFirst().get();
    }

    private ConversationThread createConversation(String id, MessageDirection direction) {
        return new ConversationThread(AD_ID, id, NOW, NOW, NOW, false,
                Optional.of("Message-Preview"), Optional.of(BUYER_NAME), Optional.of(SELLER_NAME), Optional.of(BUYER_EMAIL),
                Optional.of(direction.toString()), Optional.of(BUYER_ID), Optional.of(SELLER_ID));
    }

    private AbstractConversationThread createConversation(int number) {
        String conversationId = "conversation-" + number;
        String messagePreview = conversationId + ": This is preview!";
        DateTime date = NOW.minusDays(number);

        MessageDirection direction;
        if (number % 2 == 0) {
            direction = MessageDirection.BUYER_TO_SELLER;
        } else {
            direction = MessageDirection.SELLER_TO_BUYER;
        }

        return new ConversationThread(AD_ID, conversationId, date, date, date, false, Optional.of(messagePreview),
                Optional.of(BUYER_NAME), Optional.of(SELLER_NAME), Optional.of(BUYER_EMAIL), Optional.of(direction.toString()),
                Optional.of(BUYER_ID), Optional.of(SELLER_ID));
    }

    @Import({CassandraSimplePostBoxConfiguration.class, PostBoxOverviewController.class})
    static class TestContext {
        @Value("${persistence.cassandra.consistency.read:#{null}}")
        private ConsistencyLevel cassandraReadConsistency;

        @Value("${persistence.cassandra.consistency.write:#{null}}")
        private ConsistencyLevel cassandraWriteConsistency;

        @Bean
        public Session cassandraSessionForMb() {
            String keyspace = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();
            String[] schemas = new String[]{"cassandra_schema.cql", "cassandra_messagecenter_schema.cql"};
            return CassandraIntegrationTestProvisioner.getInstance().loadSchema(keyspace, schemas);
        }

        @Bean(name = "cassandraReadConsistency")
        public ConsistencyLevel getCassandraReadConsistency() {
            return cassandraReadConsistency;
        }

        @Bean(name = "cassandraWriteConsistency")
        public ConsistencyLevel getCassandraWriteConsistency() {
            return cassandraWriteConsistency;
        }

        @Bean
        public PropertySourcesPlaceholderConfigurer configurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
            configurer.setNullValue("null");
            return configurer;
        }

        @Bean
        public ConversationRepository conversationRepository(Session cassandraSessionForCore) {
            ConversationResumer resumer = new IdBasedConversationResumer();
            ReflectionTestUtils.setField(resumer, "userIdentifierService", new UserIdentifierConfiguration().createUserIdentifierService());

            return new DefaultCassandraConversationRepository(cassandraSessionForCore, cassandraReadConsistency, cassandraWriteConsistency, resumer, 100, 5000, false);
        }
    }
}
