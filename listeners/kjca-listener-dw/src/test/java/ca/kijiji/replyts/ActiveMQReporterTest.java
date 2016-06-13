package ca.kijiji.replyts;

import ca.kijiji.replyts.emailblockedfilter.EmailBlockedFilterFactory;
import ca.kijiji.replyts.ipblockedfilter.IpBlockedFilterFactory;
import ca.kijiji.replyts.json.JsonTransformer;
import ca.kijiji.replyts.model.ReplyTSConversationDTO;
import ca.kijiji.replyts.model.ReplyTSMessageDTO;
import ca.kijiji.replyts.model.ReplyTSProcessedMessageEventDTO;
import com.ecg.de.kleinanzeigen.replyts.userfilter.UserfilterFactory;
import com.ecg.replyts.app.filterchain.FilterChain;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.ecg.replyts.core.runtime.model.conversation.ProcessingFeedbackBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.core.JmsTemplate;

import java.util.Arrays;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ActiveMQReporterTest {
    static final String MSG_ID = "a:msg1";
    static final String CONV_ID = "a:conv1";
    static final String AD_ID = "12345";
    static final String REPLIER_EMAIL = "replier@kijiji.ca";
    static final String REPLIER_SECRET = "abc123";
    static final String REPLIER_ANON_ID = "b." + REPLIER_SECRET;
    static final String POSTER_EMAIL = "poster@kijiji.ca";
    static final String POSTER_SECRET = "qwe321";
    static final String POSTER_ANON_ID = "s." + POSTER_SECRET;
    static final String REPLIER_NAME = "Replier Name";
    static final String REPLIER_ID = "789";
    static final String POSTER_ID = "123";
    static final String HTTP_HEADER_ID = "http-header-uuid1";
    static final String EMAIL_MESSAGE_ORIGIN = "email";
    static final String R2S_BOX_MESSAGE_ORIGIN = "r2s-box";
    static final String MESSAGEBOX_MESSAGE_ORIGIN = "mb";

    @Mock
    private JmsTemplate jmsTemplate;

    private ActiveMQReporter reporter;
    private DateTime now;
    private DateTime msgReceivedDate;
    private DateTime convCreatedDate;
    private DateTime convModifiedDate;
    private ImmutableMessage.Builder msgBuilder;
    private ImmutableConversation.Builder convBuilder;
    private ReplyTSConversationDTO expectedConvDto;
    private JsonTransformer<ReplyTSProcessedMessageEventDTO> transformer = new JsonTransformer<>(ReplyTSProcessedMessageEventDTO.class, false);

    @Before
    public void setUp() throws Exception {
        reporter = new ActiveMQReporter(jmsTemplate, "b", "s", ".");

        now = DateTime.now(DateTimeZone.UTC);
        msgReceivedDate = now.minusMinutes(10);
        convCreatedDate = msgReceivedDate.minusSeconds(1);
        convModifiedDate = now;
        msgBuilder = ImmutableMessage.Builder.aMessage()
                .withId(MSG_ID)
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withState(MessageState.SENT)
                .withReceivedAt(msgReceivedDate)
                .withLastModifiedAt(convModifiedDate)
                .withTextParts(Arrays.asList(""))
                .withHeader(BoxHeaders.REPLIER_NAME.getHeaderName(), REPLIER_NAME)
                .withHeader(BoxHeaders.REPLIER_ID.getHeaderName(), REPLIER_ID)
                .withHeader(BoxHeaders.POSTER_ID.getHeaderName(), POSTER_ID);

        convBuilder = ImmutableConversation.Builder.aConversation()
                .withId(CONV_ID)
                .withAdId(AD_ID)
                .withBuyer(REPLIER_EMAIL, REPLIER_SECRET)
                .withSeller(POSTER_EMAIL, POSTER_SECRET)
                .withState(ConversationState.ACTIVE)
                .withCreatedAt(convCreatedDate)
                .withLastModifiedAt(convModifiedDate);
        expectedConvDto = new ReplyTSConversationDTO(
                CONV_ID,
                Long.valueOf(AD_ID),
                REPLIER_EMAIL,
                REPLIER_ANON_ID,
                REPLIER_NAME,
                Long.valueOf(REPLIER_ID),
                POSTER_EMAIL,
                POSTER_ANON_ID,
                Long.valueOf(POSTER_ID),
                convCreatedDate,
                convModifiedDate
        );
    }

    @Test
    public void cleanMessage_withAttachment_sentToQueue() throws Exception {
        Message message = msgBuilder
                .withHeader(BoxHeaders.HTTP_HEADER_ID.getHeaderName(), HTTP_HEADER_ID)
                .withHeader(BoxHeaders.HAS_ATTACHMENT.getHeaderName(), "true")
                .withAttachments(ImmutableList.of("attachment.txt"))
                .build();

        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(message))
                .build();

        reporter.messageProcessed(conversation, message);

        ReplyTSMessageDTO expectedMsgDto = new ReplyTSMessageDTO(
                MSG_ID, CONV_ID, HTTP_HEADER_ID, null, EMAIL_MESSAGE_ORIGIN, ReplyTSMessageDTO.Type.REPLIER_TO_POSTER,
                ReplyTSMessageDTO.MessageStatus.SENT,
                ReplyTSMessageDTO.ModerationStatus.UNCHECKED,
                true, false, false, convModifiedDate, null, null, msgReceivedDate, convModifiedDate
        );
        ReplyTSProcessedMessageEventDTO eventDto = new ReplyTSProcessedMessageEventDTO(expectedMsgDto, expectedConvDto);
        verify(jmsTemplate).convertAndSend(ActiveMQReporter.QUEUE_NAME, transformer.toJson(eventDto));
    }

    @Test
    public void cleanMessage_fromSeller_sentToQueue() throws Exception {
        Message originalMsg = msgBuilder.withId(MSG_ID + "first-reply").withHeader(BoxHeaders.HTTP_HEADER_ID.getHeaderName(), HTTP_HEADER_ID).build();
        Message reply = msgBuilder.withId(MSG_ID)
                .withHeaders(ImmutableMap.<String, String>of())
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .build();

        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(originalMsg, reply))
                .build();

        reporter.messageProcessed(conversation, reply);

        ReplyTSMessageDTO expectedMsgDto = new ReplyTSMessageDTO(
                MSG_ID, CONV_ID, null, null, EMAIL_MESSAGE_ORIGIN, ReplyTSMessageDTO.Type.POSTER_TO_REPLIER,
                ReplyTSMessageDTO.MessageStatus.SENT,
                ReplyTSMessageDTO.ModerationStatus.UNCHECKED,
                false, false, false, convModifiedDate, null, null, msgReceivedDate, convModifiedDate
        );
        ReplyTSProcessedMessageEventDTO eventDto = new ReplyTSProcessedMessageEventDTO(expectedMsgDto, expectedConvDto);
        verify(jmsTemplate).convertAndSend(ActiveMQReporter.QUEUE_NAME, transformer.toJson(eventDto));
    }

    @Test
    public void cleanMessage_fromSeller_withAttachment_sentToQueue() throws Exception {
        Message originalMsg = msgBuilder.withId(MSG_ID + "first-reply").withHeader(BoxHeaders.HTTP_HEADER_ID.getHeaderName(), HTTP_HEADER_ID).build();
        Message reply = msgBuilder.withId(MSG_ID)
                .withHeaders(ImmutableMap.<String, String>of())
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withAttachments(ImmutableList.of("attachment.txt"))
                .build();

        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(originalMsg, reply))
                .build();

        reporter.messageProcessed(conversation, reply);

        ReplyTSMessageDTO expectedMsgDto = new ReplyTSMessageDTO(
                MSG_ID, CONV_ID, null, null, EMAIL_MESSAGE_ORIGIN, ReplyTSMessageDTO.Type.POSTER_TO_REPLIER,
                ReplyTSMessageDTO.MessageStatus.SENT,
                ReplyTSMessageDTO.ModerationStatus.UNCHECKED,
                true, false, false, convModifiedDate, null, null, msgReceivedDate, convModifiedDate
        );
        ReplyTSProcessedMessageEventDTO eventDto = new ReplyTSProcessedMessageEventDTO(expectedMsgDto, expectedConvDto);
        verify(jmsTemplate).convertAndSend(ActiveMQReporter.QUEUE_NAME, transformer.toJson(eventDto));
    }

    @Test
    public void unparsableMsg_ignored() throws Exception {
        Message message = msgBuilder
                .withState(MessageState.UNPARSABLE)
                .build();
        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(message))
                .build();

        reporter.messageProcessed(conversation, message);

        verifyNoMoreInteractions(jmsTemplate);
    }

    @Test
    public void unknownDirection_ignored() throws Exception {
        Message message = msgBuilder
                .withMessageDirection(MessageDirection.UNKNOWN)
                .build();
        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(message))
                .build();

        reporter.messageProcessed(conversation, message);

        verifyNoMoreInteractions(jmsTemplate);
    }

    @Test
    public void droppedMessage_blockedEmailAddress_sentToQueue() throws Exception {
        Message message = msgBuilder
                .withState(MessageState.BLOCKED)
                .withFilterResultState(FilterResultState.DROPPED)
                .withProcessingFeedback(ProcessingFeedbackBuilder.aProcessingFeedback()
                                .withFilterInstance("userfilter")
                                .withFilterName(UserfilterFactory.class.getCanonicalName())
                )
                .withProcessingFeedback(ProcessingFeedbackBuilder.aProcessingFeedback()
                                .withFilterInstance("filterchain")
                                .withFilterName(FilterChain.class.getCanonicalName())
                )
                .build();

        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(message))
                .build();

        reporter.messageProcessed(conversation, message);

        ReplyTSMessageDTO expectedMsgDto = new ReplyTSMessageDTO(
                MSG_ID, CONV_ID, null, null, EMAIL_MESSAGE_ORIGIN, ReplyTSMessageDTO.Type.REPLIER_TO_POSTER,
                ReplyTSMessageDTO.MessageStatus.DROPPED,
                ReplyTSMessageDTO.ModerationStatus.UNCHECKED,
                false, true, false, null, null, null, msgReceivedDate, convModifiedDate
        );
        ReplyTSProcessedMessageEventDTO eventDto = new ReplyTSProcessedMessageEventDTO(expectedMsgDto, expectedConvDto);
        verify(jmsTemplate).convertAndSend(ActiveMQReporter.QUEUE_NAME, transformer.toJson(eventDto));
    }

    @Test
    public void droppedMessage_blockedUserByEmail_sentToQueue() throws Exception {
        Message message = msgBuilder
                .withState(MessageState.BLOCKED)
                .withFilterResultState(FilterResultState.DROPPED)
                .withProcessingFeedback(ProcessingFeedbackBuilder.aProcessingFeedback()
                                .withFilterInstance("user-email-filter")
                                .withFilterName(EmailBlockedFilterFactory.class.getCanonicalName())
                )
                .withProcessingFeedback(ProcessingFeedbackBuilder.aProcessingFeedback()
                                .withFilterInstance("filterchain")
                                .withFilterName(FilterChain.class.getCanonicalName())
                )
                .build();

        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(message))
                .build();

        reporter.messageProcessed(conversation, message);

        ReplyTSMessageDTO expectedMsgDto = new ReplyTSMessageDTO(
                MSG_ID, CONV_ID, null, null, EMAIL_MESSAGE_ORIGIN, ReplyTSMessageDTO.Type.REPLIER_TO_POSTER,
                ReplyTSMessageDTO.MessageStatus.DROPPED,
                ReplyTSMessageDTO.ModerationStatus.UNCHECKED,
                false, false, true, null, null, null, msgReceivedDate, convModifiedDate
        );
        ReplyTSProcessedMessageEventDTO eventDto = new ReplyTSProcessedMessageEventDTO(expectedMsgDto, expectedConvDto);
        verify(jmsTemplate).convertAndSend(ActiveMQReporter.QUEUE_NAME, transformer.toJson(eventDto));
    }

    @Test
    public void droppedMessage_blockedUserByIp_sentToQueue() throws Exception {
        Message message = msgBuilder
                .withState(MessageState.BLOCKED)
                .withFilterResultState(FilterResultState.DROPPED)
                .withProcessingFeedback(ProcessingFeedbackBuilder.aProcessingFeedback()
                                .withFilterInstance("user-ip-filter")
                                .withFilterName(IpBlockedFilterFactory.class.getCanonicalName())
                )
                .withProcessingFeedback(ProcessingFeedbackBuilder.aProcessingFeedback()
                                .withFilterInstance("filterchain")
                                .withFilterName(FilterChain.class.getCanonicalName())
                                .withUiHint(FilterResultState.DROPPED.name())
                )
                .build();

        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(message))
                .build();

        reporter.messageProcessed(conversation, message);

        ReplyTSMessageDTO expectedMsgDto = new ReplyTSMessageDTO(
                MSG_ID, CONV_ID, null, null, EMAIL_MESSAGE_ORIGIN, ReplyTSMessageDTO.Type.REPLIER_TO_POSTER,
                ReplyTSMessageDTO.MessageStatus.DROPPED,
                ReplyTSMessageDTO.ModerationStatus.UNCHECKED,
                false, false, true, null, null, null, msgReceivedDate, convModifiedDate
        );
        ReplyTSProcessedMessageEventDTO eventDto = new ReplyTSProcessedMessageEventDTO(expectedMsgDto, expectedConvDto);
        verify(jmsTemplate).convertAndSend(ActiveMQReporter.QUEUE_NAME, transformer.toJson(eventDto));
    }

    @Test
    public void moderatedBad_sentToQueue() throws Exception {
        Message message = msgBuilder
                .withState(MessageState.BLOCKED)
                .withHumanResultState(ModerationResultState.BAD)
                .build();

        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(message))
                .build();

        reporter.messageProcessed(conversation, message);

        ReplyTSMessageDTO expectedMsgDto = new ReplyTSMessageDTO(
                MSG_ID, CONV_ID, null, null, EMAIL_MESSAGE_ORIGIN, ReplyTSMessageDTO.Type.REPLIER_TO_POSTER,
                ReplyTSMessageDTO.MessageStatus.DROPPED,
                ReplyTSMessageDTO.ModerationStatus.BAD,
                false, false, false, null, null, convModifiedDate, msgReceivedDate, convModifiedDate
        );
        ReplyTSProcessedMessageEventDTO eventDto = new ReplyTSProcessedMessageEventDTO(expectedMsgDto, expectedConvDto);
        verify(jmsTemplate).convertAndSend(ActiveMQReporter.QUEUE_NAME, transformer.toJson(eventDto));
    }

    @Test
    public void moderatedGood_sentToQueue() throws Exception {
        Message message = msgBuilder
                .withState(MessageState.SENT)
                .withHumanResultState(ModerationResultState.GOOD)
                .build();

        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(message))
                .build();

        reporter.messageProcessed(conversation, message);

        ReplyTSMessageDTO expectedMsgDto = new ReplyTSMessageDTO(
                MSG_ID, CONV_ID, null, null, EMAIL_MESSAGE_ORIGIN, ReplyTSMessageDTO.Type.REPLIER_TO_POSTER,
                ReplyTSMessageDTO.MessageStatus.SENT,
                ReplyTSMessageDTO.ModerationStatus.GOOD,
                false, false, false, convModifiedDate, convModifiedDate, null, msgReceivedDate, convModifiedDate
        );
        ReplyTSProcessedMessageEventDTO eventDto = new ReplyTSProcessedMessageEventDTO(expectedMsgDto, expectedConvDto);
        verify(jmsTemplate).convertAndSend(ActiveMQReporter.QUEUE_NAME, transformer.toJson(eventDto));
    }

    @Test(expected = UncategorizedJmsException.class)
    public void moderatedGood_jmsException_rethrown() throws Exception {
        Message message = msgBuilder
                .withState(MessageState.SENT)
                .withHumanResultState(ModerationResultState.GOOD)
                .build();

        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(message))
                .build();

        ReplyTSMessageDTO expectedMsgDto = new ReplyTSMessageDTO(
                MSG_ID, CONV_ID, null, null, EMAIL_MESSAGE_ORIGIN, ReplyTSMessageDTO.Type.REPLIER_TO_POSTER,
                ReplyTSMessageDTO.MessageStatus.SENT,
                ReplyTSMessageDTO.ModerationStatus.GOOD,
                false, false, false, convModifiedDate, convModifiedDate, null, msgReceivedDate, convModifiedDate
        );
        ReplyTSProcessedMessageEventDTO eventDto = new ReplyTSProcessedMessageEventDTO(expectedMsgDto, expectedConvDto);
        doThrow(new UncategorizedJmsException("AMQ acting up")).when(jmsTemplate).convertAndSend(ActiveMQReporter.QUEUE_NAME, transformer.toJson(eventDto));

        reporter.messageProcessed(conversation, message);
    }

    @Test
    public void cleanMessage_fromMessageBox_sentToQueue() throws Exception {
        Message originalMsg = msgBuilder.withId(MSG_ID + "first-reply").withHeader(BoxHeaders.HTTP_HEADER_ID.getHeaderName(), HTTP_HEADER_ID).build();
        Message reply = msgBuilder.withId(MSG_ID)
                .withHeaders(ImmutableMap.of(BoxHeaders.MESSAGE_ORIGIN.getHeaderName(), MESSAGEBOX_MESSAGE_ORIGIN))
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .build();

        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(originalMsg, reply))
                .build();

        reporter.messageProcessed(conversation, reply);

        ReplyTSMessageDTO expectedMsgDto = new ReplyTSMessageDTO(
                MSG_ID, CONV_ID, null, null, MESSAGEBOX_MESSAGE_ORIGIN, ReplyTSMessageDTO.Type.POSTER_TO_REPLIER,
                ReplyTSMessageDTO.MessageStatus.SENT,
                ReplyTSMessageDTO.ModerationStatus.UNCHECKED,
                false, false, false, convModifiedDate, null, null, msgReceivedDate, convModifiedDate
        );
        ReplyTSProcessedMessageEventDTO eventDto = new ReplyTSProcessedMessageEventDTO(expectedMsgDto, expectedConvDto);
        verify(jmsTemplate).convertAndSend(ActiveMQReporter.QUEUE_NAME, transformer.toJson(eventDto));
    }

    @Test
    public void cleanMessage_fromR2SBox_sentToQueue() throws Exception {
        Message originalMsg = msgBuilder.withId(MSG_ID + "first-reply").withHeader(BoxHeaders.HTTP_HEADER_ID.getHeaderName(), HTTP_HEADER_ID).build();
        Message reply = msgBuilder.withId(MSG_ID)
                .withHeaders(ImmutableMap.of(BoxHeaders.MESSAGE_ORIGIN.getHeaderName(), R2S_BOX_MESSAGE_ORIGIN))
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .build();

        Conversation conversation = convBuilder
                .withMessages(ImmutableList.of(originalMsg, reply))
                .build();

        reporter.messageProcessed(conversation, reply);

        ReplyTSMessageDTO expectedMsgDto = new ReplyTSMessageDTO(
                MSG_ID, CONV_ID, null, null, R2S_BOX_MESSAGE_ORIGIN, ReplyTSMessageDTO.Type.POSTER_TO_REPLIER,
                ReplyTSMessageDTO.MessageStatus.SENT,
                ReplyTSMessageDTO.ModerationStatus.UNCHECKED,
                false, false, false, convModifiedDate, null, null, msgReceivedDate, convModifiedDate
        );
        ReplyTSProcessedMessageEventDTO eventDto = new ReplyTSProcessedMessageEventDTO(expectedMsgDto, expectedConvDto);
        verify(jmsTemplate).convertAndSend(ActiveMQReporter.QUEUE_NAME, transformer.toJson(eventDto));
    }


}
