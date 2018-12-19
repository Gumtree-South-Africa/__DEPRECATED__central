package com.ecg.replyts.core.webapi.screeningv2.converter;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import com.ecg.replyts.core.api.webapi.model.ConversationRtsStatus;
import com.ecg.replyts.core.api.webapi.model.MessageRts;
import com.ecg.replyts.core.api.webapi.model.MessageRtsDirection;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.ecg.replyts.core.api.webapi.model.ProcessingFeedbackRts;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DomainObjectConverterTest {
    private ConversationRepository conversationRepository;
    private DomainObjectConverter converter;
    private MailCloakingService mailCloakingService;

    @Before
    public void setup() {
        conversationRepository = mock(ConversationRepository.class);
        mailCloakingService = mock(MailCloakingService.class);
        when(mailCloakingService.createdCloakedMailAddress(eq(ConversationRole.Buyer), any(Conversation.class))).thenReturn(new MailAddress("buy3r"));
        when(mailCloakingService.createdCloakedMailAddress(eq(ConversationRole.Seller), any(Conversation.class))).thenReturn(new MailAddress("s3ll3r"));
        converter = new DomainObjectConverter(conversationRepository, mailCloakingService);
    }

    @Test
    public void testConvertsConversationBasicFields() throws Exception {
        Conversation conversation = setupConversationWithMessages("conv1", 0);

        ConversationRts conversationRts = converter.convertConversation(conversation);

        assertThat(conversationRts.getId(), equalTo("conv1"));
        assertThat(conversationRts.getAdId(), equalTo("100"));
        assertThat(conversationRts.getBuyer(), equalTo("buyer@email.com"));
        assertThat(conversationRts.getBuyerAnonymousEmail(), equalTo("buy3r"));
        assertThat(conversationRts.getSeller(), equalTo("seller@email.com"));
        assertThat(conversationRts.getSellerAnonymousEmail(), equalTo("s3ll3r"));
        assertThat(conversationRts.getCreationDate().toGMTString(), equalTo("12 Sep 2012 14:45:14 GMT"));
        assertThat(conversationRts.getLastUpdateDate().toGMTString(), equalTo("13 Sep 2012 14:45:14 GMT"));
        assertThat(conversationRts.getConversationHeader("header1"), equalTo("value1"));
        assertThat(conversationRts.getStatus(), equalTo(ConversationRtsStatus.ACTIVE));
    }

    @Test
    public void testDoesNotRecursivelyAttachConversation() throws Exception {
        Conversation conversation = setupConversationWithMessages("conv2", 3);

        ConversationRts conversationRts = converter.convertConversation(conversation);

        assertThat(conversationRts.getId(), equalTo("conv2"));
        assertThat(conversationRts.getMessages().size(), equalTo(3));

        MessageRts message1 = conversationRts.getMessages().get(0);
        MessageRts message2 = conversationRts.getMessages().get(1);
        MessageRts message3 = conversationRts.getMessages().get(2);

        assertThat(message1.getId(), equalTo("msg0"));
        assertThat(message2.getId(), equalTo("msg1"));
        assertThat(message3.getId(), equalTo("msg2"));

        assertNull(message1.getConversation());
        assertNull(message2.getConversation());
        assertNull(message3.getConversation());
    }

    @Test
    public void testConvertMessageResultsAddsConversation() throws Exception {
        setupConversationWithMessages("conv3", 2);
        setupConversationWithMessages("conv4", 3);

        List<RtsSearchResponse.IDHolder> messageResultIds = new ArrayList<RtsSearchResponse.IDHolder>();
        messageResultIds.add(new RtsSearchResponse.IDHolder("msg0", "conv3"));
        messageResultIds.add(new RtsSearchResponse.IDHolder("msg1", "conv3"));
        messageResultIds.add(new RtsSearchResponse.IDHolder("msg0", "conv4"));
        messageResultIds.add(new RtsSearchResponse.IDHolder("msg1", "conv4"));
        messageResultIds.add(new RtsSearchResponse.IDHolder("msg2", "conv4"));

        List<MessageRts> messageRtsList = converter.convertFromSearchResults(messageResultIds);

        assertThat(messageRtsList.size(), equalTo(5));

        assertNotNull(messageRtsList.get(0).getConversation());
        assertThat(messageRtsList.get(0).getConversation().getId(), equalTo("conv3"));
        assertNotNull(messageRtsList.get(1).getConversation());
        assertThat(messageRtsList.get(1).getConversation().getId(), equalTo("conv3"));
        assertNotNull(messageRtsList.get(2).getConversation());
        assertThat(messageRtsList.get(2).getConversation().getId(), equalTo("conv4"));
        assertNotNull(messageRtsList.get(3).getConversation());
        assertThat(messageRtsList.get(3).getConversation().getId(), equalTo("conv4"));
        assertNotNull(messageRtsList.get(4).getConversation());
        assertThat(messageRtsList.get(4).getConversation().getId(), equalTo("conv4"));
    }

    @Test
    public void testConvertMessageResultsSetsMessageBasicFieldsCorrectly() {
        setupConversationWithMessages("conv5", 1);

        List<RtsSearchResponse.IDHolder> messageResultIds = new ArrayList<RtsSearchResponse.IDHolder>();
        messageResultIds.add(new RtsSearchResponse.IDHolder("msg0", "conv5"));

        List<MessageRts> messageRtsList = converter.convertFromSearchResults(messageResultIds);

        assertThat(messageRtsList.size(), equalTo(1));
        MessageRts messageRts = messageRtsList.get(0);

        assertThat(messageRts.getId(), equalTo("msg0"));
        assertThat(messageRts.getReceivedDate().toGMTString(), equalTo("13 Sep 2012 14:54:14 GMT"));
        assertThat(messageRts.getFilterResultState(), equalTo(FilterResultState.OK));
        assertThat(messageRts.getHumanResultState(), equalTo(ModerationResultState.GOOD));
        assertThat(messageRts.getState(), equalTo(MessageRtsState.SENT));
        assertThat(messageRts.getText(), equalTo("some message text"));
        assertThat(messageRts.getMessageDirection(), equalTo(MessageRtsDirection.BUYER_TO_SELLER));
    }


    @Test
    public void testConvertMessageResultsSetsConversationFieldsCorrectly() {
        setupConversationWithMessages("conv6", 1);

        List<RtsSearchResponse.IDHolder> messageResultIds = new ArrayList<RtsSearchResponse.IDHolder>();
        messageResultIds.add(new RtsSearchResponse.IDHolder("msg0", "conv6"));

        List<MessageRts> messageRtsList = converter.convertFromSearchResults(messageResultIds);

        assertThat(messageRtsList.size(), equalTo(1));
        MessageRts messageRts = messageRtsList.get(0);

        ConversationRts conversationRts = messageRts.getConversation();
        assertNotNull(conversationRts);

        assertThat(conversationRts.getId(), equalTo("conv6"));
        assertThat(conversationRts.getAdId(), equalTo("100"));
        assertThat(conversationRts.getBuyer(), equalTo("buyer@email.com"));
        assertThat(conversationRts.getBuyerAnonymousEmail(), equalTo("buy3r"));
        assertThat(conversationRts.getSeller(), equalTo("seller@email.com"));
        assertThat(conversationRts.getSellerAnonymousEmail(), equalTo("s3ll3r"));
        assertThat(conversationRts.getCreationDate().toGMTString(), equalTo("12 Sep 2012 14:45:14 GMT"));
        assertThat(conversationRts.getLastUpdateDate().toGMTString(), equalTo("13 Sep 2012 14:45:14 GMT"));
        assertThat(conversationRts.getConversationHeader("header1"), equalTo("value1"));
        assertThat(conversationRts.getStatus(), equalTo(ConversationRtsStatus.ACTIVE));
    }

    @Test
    public void testConvertMessageResultsSetsProcessingFeedbackFieldsCorrectly() {
        setupConversationWithMessages("conv7", 1);

        List<RtsSearchResponse.IDHolder> messageResultIds = new ArrayList<RtsSearchResponse.IDHolder>();
        messageResultIds.add(new RtsSearchResponse.IDHolder("msg0", "conv7"));

        List<MessageRts> messageRtsList = converter.convertFromSearchResults(messageResultIds);

        assertThat(messageRtsList.size(), equalTo(1));
        MessageRts messageRts = messageRtsList.get(0);

        ProcessingFeedbackRts feedback1 = messageRts.getProcessingFeedback().get(0);
        ProcessingFeedbackRts feedback2 = messageRts.getProcessingFeedback().get(1);

        assertThat(feedback1.getFilterInstance(), equalTo("somefilterinstance"));
        assertThat(feedback1.getFilterName(), equalTo("somefiltername"));
        assertThat(feedback1.getState(), equalTo(FilterResultState.OK));
        assertThat(feedback1.getScore(), equalTo(5));
        assertThat(feedback1.getUiHint(), equalTo("uihint"));
        assertThat(feedback1.getDescription(), equalTo("feedback description"));

        assertThat(feedback2.getFilterInstance(), equalTo("somefilterinstance"));
        assertThat(feedback2.getFilterName(), equalTo("somefiltername"));
        assertThat(feedback2.getState(), equalTo(FilterResultState.OK));
        assertThat(feedback2.getScore(), equalTo(5));
        assertThat(feedback2.getUiHint(), equalTo("uihint"));
        assertThat(feedback2.getDescription(), equalTo("feedback description"));
    }


    private Conversation setupConversationWithMessages(String conversationId, int howManyMessages) {
        Map<String, String> dummyHeaderMap = new HashMap<String, String>();
        dummyHeaderMap.put("header1", "value1");
        MutableConversation conversation = mock(MutableConversation.class);
        when(conversation.getId()).thenReturn(conversationId);
        when(conversation.getAdId()).thenReturn("100");
        when(conversation.getBuyerId()).thenReturn("buyer@email.com");
        when(conversation.getSellerId()).thenReturn("seller@email.com");
        when(conversation.getBuyerSecret()).thenReturn("buy3r");
        when(conversation.getSellerSecret()).thenReturn("s3ll3r");
        when(conversation.getCreatedAt()).thenReturn(new DateTime(2012, 9, 12, 16, 45, 14, DateTimeZone.forID("Europe/Amsterdam")));
        when(conversation.getLastModifiedAt()).thenReturn(new DateTime(2012, 9, 13, 16, 45, 14, DateTimeZone.forID("Europe/Amsterdam")));
        when(conversation.getCustomValues()).thenReturn(dummyHeaderMap);
        when(conversation.getState()).thenReturn(ConversationState.ACTIVE);

        List<Message> messageList = new ArrayList<Message>();

        for (int i = 0; i < howManyMessages; i++) {
            String messageId = "msg" + i;
            Message message = mock(Message.class);
            when(message.getId()).thenReturn(messageId);
            when(message.getReceivedAt()).thenReturn(new DateTime(2012, 9, 13, 16, 54, 14, DateTimeZone.forID("Europe/Amsterdam")));
            when(message.getState()).thenReturn(MessageState.SENT);
            when(message.getFilterResultState()).thenReturn(FilterResultState.OK);
            when(message.getHumanResultState()).thenReturn(ModerationResultState.GOOD);
            when(message.getCaseInsensitiveHeaders()).thenReturn(dummyHeaderMap);
            when(message.getPlainTextBody()).thenReturn("some message text");
            when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
            when(message.getLastEditor()).thenReturn(Optional.empty());

            // 2 feedbacks
            ProcessingFeedback feedback1 = makeFeedback();
            ProcessingFeedback feedback2 = makeFeedback();
            when(message.getProcessingFeedback()).thenReturn(Arrays.asList(feedback1, feedback2));

            messageList.add(message);
            when(conversation.getMessageById(messageId)).thenReturn(message);
        }

        when(conversation.getMessages()).thenReturn(messageList);

        when(conversationRepository.getById(conversationId)).thenReturn(conversation);

        return conversation;
    }

    private ProcessingFeedback makeFeedback() {
        ProcessingFeedback feedback = mock(ProcessingFeedback.class);

        when(feedback.getFilterInstance()).thenReturn("somefilterinstance");
        when(feedback.getFilterName()).thenReturn("somefiltername");
        when(feedback.getResultState()).thenReturn(FilterResultState.OK);
        when(feedback.getScore()).thenReturn(5);
        when(feedback.getUiHint()).thenReturn("uihint");
        when(feedback.getDescription()).thenReturn("feedback description");

        return feedback;
    }
}
