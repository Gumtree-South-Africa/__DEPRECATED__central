package com.ecg.messagebox.service;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.events.MessageAddedEventProcessor;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.messagebox.util.MessagePreProcessor;
import com.ecg.messagebox.util.messages.DefaultMessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CassandraPostBoxServiceTest.TestContext.class)
@TestPropertySource(properties = {
        "message.normalization.pattern.0=\\n.*(a|b)-.*?@mail.marktplaats.nl.*\\n",
        "message.normalization.pattern.1=(Aan|To)\\s?:.*?@.*?",
        "message.normalization.pattern.2=(Subject|Onderwerp)\\s?:.*?",
        "message.normalization.pattern.3=\\w*(?<!=[?=\\n])(Date|Datum)\\s?:.*?",
        "message.normalization.pattern.4=\\n.*<[^<>\\s]+@gmail.[^<>\\s]+>.*\\n",
        "message.normalization.pattern.5=\\b(?:<b>)?(From|To|Sender|Receiver|Van|Aan) *: *(?:</b>)? *<a[^>]+href=\"mailto:[^\">]+@[^\">]+\"[^>]*>[^<]*</a",
        "message.normalization.pattern.6=\\b(?:<b>)?(From|To|Sender|Receiver|Van|Aan) *: *(?:</b>)? *(?:<[:a-z]+[^>]*>)?[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>)?",
        "message.normalization.pattern.7=<span[^>]*>(From|To|Sender|Receiver|Van|Aan) *: *</span *>[^<>]*(?:<[:a-z]+[^>]*>){0,2}[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>){0,2}",
        "message.normalization.pattern.8=<b><span[^>]*>(From|To|Sender|Receiver|Van|Aan) *: *</span *></b> *(?:<[:a-z]+[^>]*>)?[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>)?",
        "message.normalization.pattern.9=<span[^>]*><b>(From|To|Sender|Receiver|Van|Aan) *: *</b></span *> *(?:<[:a-z]+[^>]*>)?[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>)?",
        "message.normalization.pattern.10=\\b(From|To|Sender|Receiver|Van|Aan) *: *(<|&lt;)?[^<>\\s]+@[^<>\\s]+(>|&gt;)?",
        "message.normalization.pattern.11=\\b(From|To|Sender|Receiver|Van|Aan) *: *([^<>\\s]+ +){1,6}(<|&lt;)?[^<>\\s]+@[^<>\\s]+((<|&lt;)[^<>\\s]+@[^<>\\s]+(>|&gt;))?(>|&gt;)?",
        "message.normalization.pattern.12=\\b(From|To|Sender|Receiver|Van|Aan) *: *([^<>\\s]+ +){0,5}([^<>\\s]+)(<|&lt;)?[^<>\\s]+@[^<>\\s]+(>|&gt;)?",
        "message.normalization.pattern.13=Op.{10,25}schreef[^<]{5,60}<a[^>]+href=\"mailto:[^\">]+@[^\">]+\"[^>]*>[^<]*</a",
        "message.normalization.pattern.14=Op.{10,25}schreef[^<]{5,60}(<|&lt;)?\\s*[^<>\\s]+@[^<>\\s]+(>|&gt;)?",
        "message.normalization.pattern.15=Am [0-9][0-9][0-9]?[0-9]?[./-].* schrieb.*",
        "message.normalization.pattern.16=On [0-9][0-9][0-9]?[0-9]?[./-].* wrote.*",
        "message.normalization.pattern.17=[0-9][0-9][0-9]?[0-9]?[./-].*buyer-.*@mail.mobile.de",
        "message.normalization.pattern.18=[0-9][0-9][0-9]?[0-9]?[./-].*seller-.*@mail.mobile.de"
})
public class CassandraPostBoxServiceTest {
    private static final String USER_ID_1 = "1";
    private static final String USER_ID_2 = "2";
    private static final String DEFAULT_SUBJECT = "Default subject";
    private static final String AD_ID_1 = "m123";
    private static final String AD_ID_2 = "m152";
    private static final String CONVERSATION_ID_1 = "c1";
    private static final String CONVERSATION_ID_2 = "c2";

    private static final String BUYER_USER_ID_NAME = "user-id-buyer";
    private static final String SELLER_USER_ID_NAME = "user-id-seller";

    private static final String BUYER_NAME_KEY = "buyer-name";
    private static final String SELLER_NAME_KEY = "seller-name";

    private static final String BUYER_NAME_VALUE = "buyer";
    private static final String SELLER_NAME_VALUE = "seller";

    private static final String CURSOR = "msg1234";
    private static final int MESSAGE_LIMIT = 100;

    private static final int CONVERSATION_OFFSET = 0;
    private static final int CONVERSATION_LIMIT = 50;

    private static final UserUnreadCounts UNREAD_COUNTS = new UserUnreadCounts(USER_ID_1, 2, 5);

    @Mock
    private CassandraPostBoxRepository postBoxRepo;
    @Mock
    private UserIdentifierService userIdentifierService;
    @Mock
    private BlockUserRepository blockUserRepo;
    @Mock
    private ResponseDataCalculator responseDataCalculator;
    @Mock
    private PostBox postBox;
    @Mock
    private MessageAddedEventProcessor messageAddedEventProcessor;
    @Mock
    private ConversationRepository conversationRepo;
    @Mock
    private MutableConversation conversation;

    private CassandraPostBoxService service;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        DateTimeUtils.setCurrentMillisFixed(now().getMillis());
        service = new CassandraPostBoxService(postBoxRepo, userIdentifierService,
                responseDataCalculator, messageAddedEventProcessor, conversationRepo);
        when(userIdentifierService.getBuyerUserIdName()).thenReturn(BUYER_USER_ID_NAME);
        when(userIdentifierService.getSellerUserIdName()).thenReturn(SELLER_USER_ID_NAME);
        when(blockUserRepo.areUsersBlocked(any(), any())).thenReturn(false);
        when(postBoxRepo.getConversationMessageNotification(USER_ID_1, CONVERSATION_ID_1)).thenReturn(empty());
        when(postBoxRepo.getUserUnreadCounts(USER_ID_1)).thenReturn(UNREAD_COUNTS);
        when(conversationRepo.getById(CONVERSATION_ID_1)).thenReturn(conversation);
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void processNewMessageWithCorrectSubject() {
        Message rtsMsg1 = newMessage("1", SELLER_TO_BUYER, MessageState.SENT, DEFAULT_SUBJECT);
        Message rtsMsg2 = newMessage("2", SELLER_TO_BUYER, MessageState.SENT, "Another subject");
        Conversation conversation = newConversationWithMessages(CONVERSATION_ID_1, singletonList(rtsMsg1)).build();

        when(userIdentifierService.getBuyerUserId(conversation)).thenReturn(Optional.of(USER_ID_1));
        when(userIdentifierService.getSellerUserId(conversation)).thenReturn(Optional.of(USER_ID_2));

        service.processNewMessage(USER_ID_1, conversation, rtsMsg2, true, "");

        ArgumentCaptor<ConversationThread> conversationThreadArgCaptor = ArgumentCaptor.forClass(ConversationThread.class);
        ArgumentCaptor<com.ecg.messagebox.model.Message> messageArgCaptor = ArgumentCaptor.forClass(com.ecg.messagebox.model.Message.class);
        verify(postBoxRepo).createConversation(eq(USER_ID_1), conversationThreadArgCaptor.capture(), messageArgCaptor.capture(), anyBoolean());

        ConversationThread capturedConversationThread = conversationThreadArgCaptor.getValue();
        Assert.assertEquals(DEFAULT_SUBJECT, capturedConversationThread.getMetadata().getEmailSubject());
        Assert.assertEquals(CONVERSATION_ID_1, capturedConversationThread.getId());
        Assert.assertEquals(AD_ID_1, capturedConversationThread.getAdId());
        Assert.assertEquals(Visibility.ACTIVE, capturedConversationThread.getVisibility());
        Assert.assertEquals(MessageNotification.RECEIVE, capturedConversationThread.getMessageNotification());

        DateTime messageIdDate = new DateTime(UUIDs.unixTimestamp(messageArgCaptor.getValue().getId()));
        DateTime currentTime = new DateTime();
        Assert.assertEquals(messageIdDate.dayOfMonth(), currentTime.dayOfMonth());
        Assert.assertEquals(messageIdDate.monthOfYear(), currentTime.monthOfYear());
        Assert.assertEquals(messageIdDate.year(), currentTime.year());

        verify(responseDataCalculator).storeResponseData(USER_ID_1, conversation, rtsMsg2);
    }

    @Test
    public void processNewMessageWithMetadataHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Message-Type", "asq");
        headers.put("X-Message-Metadata", "metadata");
        headers.put("X-Conversation-Title", "conversation title");
        headers.put("Subject", "subject");

        Message rtsMsg = newMessageWithHeaders("1", SELLER_TO_BUYER, MessageState.SENT, headers);

        ArgumentCaptor<com.ecg.messagebox.model.Message> messageArgCaptor = ArgumentCaptor.forClass(com.ecg.messagebox.model.Message.class);

        Conversation rtsConversation = newConversation(CONVERSATION_ID_1).withMessages(singletonList(rtsMsg)).build();

        List<Participant> participants = newArrayList(
                new Participant(USER_ID_1, BUYER_NAME_VALUE, rtsConversation.getBuyerId(), ParticipantRole.BUYER),
                new Participant(USER_ID_2, SELLER_NAME_VALUE, rtsConversation.getSellerId(), ParticipantRole.SELLER));

        ArgumentCaptor<ConversationThread> conversationThreadArgCaptor = ArgumentCaptor.forClass(ConversationThread.class);

        when(userIdentifierService.getBuyerUserId(rtsConversation)).thenReturn(Optional.of(USER_ID_1));
        when(userIdentifierService.getSellerUserId(rtsConversation)).thenReturn(Optional.of(USER_ID_2));

        service.processNewMessage(USER_ID_1, rtsConversation, rtsMsg, true, "text 123");

        verify(postBoxRepo).createConversation(eq(USER_ID_1), conversationThreadArgCaptor.capture(), messageArgCaptor.capture(), eq(true));

        com.ecg.messagebox.model.Message capturedMessage = messageArgCaptor.getValue();

        DateTime messageIdDate = new DateTime(UUIDs.unixTimestamp(capturedMessage.getId()));
        DateTime currentTime = new DateTime();
        Assert.assertEquals(messageIdDate.dayOfMonth(), currentTime.dayOfMonth());
        Assert.assertEquals(messageIdDate.monthOfYear(), currentTime.monthOfYear());
        Assert.assertEquals(messageIdDate.year(), currentTime.year());

        Assert.assertEquals("text 123", capturedMessage.getText());
        Assert.assertEquals(USER_ID_2, capturedMessage.getSenderUserId());
        Assert.assertEquals(MessageType.ASQ, capturedMessage.getType());
        Assert.assertEquals("metadata", capturedMessage.getCustomData());

        ConversationThread conversation = conversationThreadArgCaptor.getValue();
        Assert.assertEquals(conversation.getId(), rtsConversation.getId());
        Assert.assertEquals(conversation.getAdId(), rtsConversation.getAdId());
        Assert.assertEquals(conversation.getVisibility(), Visibility.ACTIVE);
        Assert.assertEquals(conversation.getMessageNotification(), MessageNotification.RECEIVE);
        Assert.assertEquals(conversation.getParticipants(), participants);
    }


    @Test
    public void processNewMessageWithMetadataImageUrlHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Message-Type", "asq");
        headers.put("X-Message-Metadata", "metadata");
        headers.put("X-Conversation-Title", "conversation title");
        headers.put("X-Conversation-Image-Url", "conversation.image.url");
        headers.put("Subject", "subject");

        Message rtsMsg = newMessageWithHeaders("1", SELLER_TO_BUYER, MessageState.SENT, headers);

        ArgumentCaptor<com.ecg.messagebox.model.Message> messageArgCaptor = ArgumentCaptor.forClass(com.ecg.messagebox.model.Message.class);

        Conversation rtsConversation = newConversation(CONVERSATION_ID_1).withMessages(singletonList(rtsMsg)).build();

        ArgumentCaptor<ConversationThread> conversationThreadArgCaptor = ArgumentCaptor.forClass(ConversationThread.class);

        when(userIdentifierService.getBuyerUserId(rtsConversation)).thenReturn(Optional.of(USER_ID_1));
        when(userIdentifierService.getSellerUserId(rtsConversation)).thenReturn(Optional.of(USER_ID_2));

        service.processNewMessage(USER_ID_1, rtsConversation, rtsMsg, true, "text 123");

        verify(postBoxRepo).createConversation(eq(USER_ID_1), conversationThreadArgCaptor.capture(), messageArgCaptor.capture(), eq(true));

        ConversationThread conversation = conversationThreadArgCaptor.getValue();
        Assert.assertEquals(conversation.getMetadata().getImageUrl(), "conversation.image.url");
    }

    @Test
    public void processNewMessageWithIdHeader() {
        when(postBoxRepo.getConversationMessageNotification(USER_ID_1, CONVERSATION_ID_1)).thenReturn(empty());

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Message-Type", "asq");
        headers.put("X-Message-ID", "f866b110-857b-11e6-9367-5bbf510138cd");
        headers.put("X-Conversation-Title", "title");
        headers.put("Subject", "subject");
        Message rtsMsg = newMessageWithHeaders("1", SELLER_TO_BUYER, MessageState.SENT, headers);

        com.ecg.messagebox.model.Message newMessage = new com.ecg.messagebox.model.Message(
                UUID.fromString("f866b110-857b-11e6-9367-5bbf510138cd"), "text 123", USER_ID_2, MessageType.ASQ, null);

        Conversation rtsConversation = newConversation(CONVERSATION_ID_1).withMessages(singletonList(rtsMsg)).build();

        when(userIdentifierService.getBuyerUserId(rtsConversation)).thenReturn(Optional.of(USER_ID_1));
        when(userIdentifierService.getSellerUserId(rtsConversation)).thenReturn(Optional.of(USER_ID_2));

        List<Participant> participants = newArrayList(
                new Participant(USER_ID_1, BUYER_NAME_VALUE, rtsConversation.getBuyerId(), ParticipantRole.BUYER),
                new Participant(USER_ID_2, SELLER_NAME_VALUE, rtsConversation.getSellerId(), ParticipantRole.SELLER));

        ConversationThread conversation = new ConversationThread(
                rtsConversation.getId(),
                rtsConversation.getAdId(),
                USER_ID_1,
                Visibility.ACTIVE,
                MessageNotification.RECEIVE,
                participants,
                newMessage,
                new ConversationMetadata(now(), "subject", "title", null));

        service.processNewMessage(USER_ID_1, rtsConversation, rtsMsg, true, "text 123");

        verify(postBoxRepo).createConversation(USER_ID_1, conversation, newMessage, true);
    }

    @Test
    public void getConversation() {
        ConversationThread expectedConversationThread = newConversationThread(CONVERSATION_ID_1);
        when(postBoxRepo.getConversationWithMessages(USER_ID_1, CONVERSATION_ID_1, CURSOR, MESSAGE_LIMIT)).thenReturn(Optional.of(expectedConversationThread));
        Optional<ConversationThread> conversationThread = service.getConversation(USER_ID_1, CONVERSATION_ID_1, CURSOR, MESSAGE_LIMIT);

        Assert.assertEquals(expectedConversationThread, conversationThread.get());
    }


    @Test
    public void markConversationAsRead() {
        ConversationThread repoConversationThread = newConversationThread(CONVERSATION_ID_1).addNumUnreadMessages(USER_ID_1, 5);
        when(postBoxRepo.getConversationWithMessages(USER_ID_1, CONVERSATION_ID_1, CURSOR, MESSAGE_LIMIT)).thenReturn(Optional.of(repoConversationThread));

        Optional<ConversationThread> conversationThread = service.markConversationAsRead(USER_ID_1, CONVERSATION_ID_1, CURSOR, MESSAGE_LIMIT);

        ConversationThread expectedConversationThread = repoConversationThread.addNumUnreadMessages(USER_ID_1, 0);

        verify(postBoxRepo).resetConversationUnreadCount(USER_ID_1, USER_ID_2, CONVERSATION_ID_1, AD_ID_1);
        Assert.assertEquals(expectedConversationThread, conversationThread.get());
    }

    @Test
    public void getConversations() {
        PostBox expectedPostBox = newPostBox();
        when(postBoxRepo.getPostBox(USER_ID_1, Visibility.ACTIVE, CONVERSATION_OFFSET, CONVERSATION_LIMIT)).thenReturn(expectedPostBox);
        PostBox postBox = service.getConversations(USER_ID_1, Visibility.ACTIVE, CONVERSATION_OFFSET, CONVERSATION_LIMIT);

        Assert.assertEquals(expectedPostBox, postBox);
    }

    @Test
    public void archiveConversation() {
        List<String> conversationIds = Arrays.asList(CONVERSATION_ID_1, CONVERSATION_ID_2);
        Map<String, String> conversationAdIdsMap = new HashMap<>();
        conversationAdIdsMap.put(CONVERSATION_ID_1, AD_ID_1);
        conversationAdIdsMap.put(CONVERSATION_ID_2, AD_ID_2);

        PostBox expectedPostBox = newPostBox();

        when(postBoxRepo.getConversationAdIdsMap(USER_ID_1, conversationIds)).thenReturn(conversationAdIdsMap);
        when(postBoxRepo.getPostBox(USER_ID_1, Visibility.ACTIVE, CONVERSATION_OFFSET, CONVERSATION_LIMIT)).thenReturn(postBox);
        when(postBox.removeConversations(conversationIds)).thenReturn(expectedPostBox);

        PostBox returnedPostBox = service.archiveConversations(USER_ID_1, conversationIds, CONVERSATION_OFFSET, CONVERSATION_LIMIT);

        verify(postBoxRepo).archiveConversations(USER_ID_1, conversationAdIdsMap);
        Assert.assertEquals(expectedPostBox, returnedPostBox);
    }

    @Test
    public void activateConversation() {
        List<String> conversationIds = Arrays.asList(CONVERSATION_ID_1, CONVERSATION_ID_2);
        Map<String, String> conversationAdIdsMap = new HashMap<>();
        conversationAdIdsMap.put(CONVERSATION_ID_1, AD_ID_1);
        conversationAdIdsMap.put(CONVERSATION_ID_2, AD_ID_2);

        PostBox expectedPostBox = newPostBox();

        when(postBoxRepo.getConversationAdIdsMap(USER_ID_1, conversationIds)).thenReturn(conversationAdIdsMap);
        when(postBoxRepo.getPostBox(USER_ID_1, Visibility.ARCHIVED, CONVERSATION_OFFSET, CONVERSATION_LIMIT)).thenReturn(postBox);
        when(postBox.removeConversations(conversationIds)).thenReturn(expectedPostBox);

        PostBox returnedPostBox = service.activateConversations(USER_ID_1, conversationIds, CONVERSATION_OFFSET, CONVERSATION_LIMIT);

        verify(postBoxRepo).activateConversations(USER_ID_1, conversationAdIdsMap);
        Assert.assertEquals(expectedPostBox, returnedPostBox);
    }


    @Test
    public void getUnreadCounts() {
        UserUnreadCounts expectedUserUnreadCounts = new UserUnreadCounts(USER_ID_1, 3, 5);
        when(postBoxRepo.getUserUnreadCounts(USER_ID_1)).thenReturn(expectedUserUnreadCounts);

        UserUnreadCounts userUnreadCounts = service.getUnreadCounts(USER_ID_1);

        Assert.assertEquals(expectedUserUnreadCounts, userUnreadCounts);
    }

    @Test
    public void deleteConversation() {
        service.deleteConversation(USER_ID_1, CONVERSATION_ID_1, AD_ID_1);

        verify(postBoxRepo).deleteConversation(USER_ID_1, CONVERSATION_ID_1, AD_ID_1);
    }

    @Test
    public void createSystemMessage() {
        service.createSystemMessage(USER_ID_1, CONVERSATION_ID_1, AD_ID_1, "text", "custom data", false);

        ArgumentCaptor<com.ecg.messagebox.model.Message> captor = ArgumentCaptor.forClass(com.ecg.messagebox.model.Message.class);

        verify(postBoxRepo).addSystemMessage(eq(USER_ID_1), eq(CONVERSATION_ID_1), eq(AD_ID_1), captor.capture());

        com.ecg.messagebox.model.Message verifyMessage = captor.getValue();

        Assert.assertEquals("text", verifyMessage.getText());
        Assert.assertEquals(CassandraPostBoxService.SYSTEM_MESSAGE_USER_ID, verifyMessage.getSenderUserId());
        Assert.assertEquals(MessageType.SYSTEM_MESSAGE, verifyMessage.getType());
        Assert.assertEquals("custom data", verifyMessage.getCustomData());
        verifyZeroInteractions(messageAddedEventProcessor);
    }

    @Test
    public void createSystemMessageWithSendPush() {
        service.createSystemMessage(USER_ID_1, CONVERSATION_ID_1, AD_ID_1, "text", "custom data", true);

        ArgumentCaptor<com.ecg.messagebox.model.Message> captor = ArgumentCaptor.forClass(com.ecg.messagebox.model.Message.class);

        verify(postBoxRepo).addSystemMessage(eq(USER_ID_1), eq(CONVERSATION_ID_1), eq(AD_ID_1), captor.capture());

        com.ecg.messagebox.model.Message verifyMessage = captor.getValue();

        Assert.assertEquals("text", verifyMessage.getText());
        Assert.assertEquals(CassandraPostBoxService.SYSTEM_MESSAGE_USER_ID, verifyMessage.getSenderUserId());
        Assert.assertEquals(MessageType.SYSTEM_MESSAGE, verifyMessage.getType());
        Assert.assertEquals("custom data", verifyMessage.getCustomData());
        verify(messageAddedEventProcessor).publishMessageAddedEvent(eq(conversation), any(String.class), eq("text"), eq(UNREAD_COUNTS));
    }

    private ImmutableConversation.Builder newConversation(String id) {
        return aConversation()
                .withId(id)
                .withAdId(AD_ID_1)
                .withCreatedAt(new DateTime())
                .withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE)
                .withCustomValues(ImmutableMap.of(BUYER_USER_ID_NAME, USER_ID_1, SELLER_USER_ID_NAME, USER_ID_2,
                        BUYER_NAME_KEY, BUYER_NAME_VALUE, SELLER_NAME_KEY, SELLER_NAME_VALUE));
    }

    private ImmutableConversation.Builder newConversationWithMessages(String id, List<Message> messageList) {
        return newConversation(id).withMessages(messageList);
    }

    private Message newMessage(String id, MessageDirection direction, MessageState state, String subject) {
        return aMessage()
                .withId(id)
                .withEventTimeUUID(UUID.randomUUID())
                .withMessageDirection(direction)
                .withState(state)
                .withReceivedAt(new DateTime(2016, 1, 30, 20, 11, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2016, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withHeader("X-Message-Type", "asq")
                .withTextParts(singletonList("text 123"))
                .withHeader("Subject", subject)
                .build();
    }

    private Message newMessageWithHeaders(String id, MessageDirection direction, MessageState state, Map<String, String> headers) {
        return aMessage()
                .withId(id)
                .withEventTimeUUID(UUIDs.timeBased())
                .withMessageDirection(direction)
                .withState(state)
                .withReceivedAt(new DateTime(2016, 1, 30, 20, 11, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2016, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withHeaders(headers)
                .withTextParts(singletonList("text 123"))
                .build();
    }

    private ConversationThread newConversationThread(String conversationId) {
        return new ConversationThread(
                conversationId,
                AD_ID_1,
                USER_ID_1,
                Visibility.ACTIVE,
                MessageNotification.RECEIVE,
                Arrays.asList(new Participant(USER_ID_1, "user1", "user1@email.test", ParticipantRole.BUYER),
                        new Participant(USER_ID_2, "user2", "user2@email.test", ParticipantRole.SELLER)),
                new com.ecg.messagebox.model.Message(UUIDs.timeBased(), MessageType.CHAT, new MessageMetadata("text", "senderUserId")),
                new ConversationMetadata(DateTime.now(), "subject", "title", null)
        );
    }

    private PostBox newPostBox() {
        return new PostBox(
                USER_ID_1,
                Arrays.asList(newConversationThread(CONVERSATION_ID_1), newConversationThread(CONVERSATION_ID_2)),
                new UserUnreadCounts(USER_ID_1, 2, 5),
                6
        );
    }

    @Configuration
    @Import({DefaultMessagesResponseFactory.class, MessagePreProcessor.class})
    static class TestContext {
    }
}