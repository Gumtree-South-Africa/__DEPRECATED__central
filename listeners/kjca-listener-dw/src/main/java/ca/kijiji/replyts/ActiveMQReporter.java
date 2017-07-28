package ca.kijiji.replyts;

import ca.kijiji.replyts.emailblockedfilter.EmailBlockedFilterFactory;
import ca.kijiji.replyts.ipblockedfilter.IpBlockedFilterFactory;
import ca.kijiji.replyts.json.JsonTransformer;
import ca.kijiji.replyts.model.ReplyTSConversationDTO;
import ca.kijiji.replyts.model.ReplyTSMessageDTO;
import ca.kijiji.replyts.model.ReplyTSProcessedMessageEventDTO;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.de.kleinanzeigen.replyts.userfilter.UserfilterFactory;
import com.ecg.replyts.app.filterchain.FilterChain;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Component
/*
 * Listens for message-processed events and reports current conversation and message
 * states to the data warehouse queue, which is processed in Box's Batch runner.
 */
public class ActiveMQReporter implements MessageProcessedListener {
    static final String QUEUE_NAME = "replytsMessageProcessedEventQueue";
    private static final Timer REPORTER_TIMER = TimingReports.newTimer("amq-reporter");
    private static final Counter JMS_EXCEPTION_COUNTER = TimingReports.newCounter("amq-reporter.exceptions");

    private static final String EMAIL_MESSAGE_ORIGIN = "email";

    private final JmsTemplate jmsTemplate;
    private final String buyerPrefix;
    private final String sellerPrefix;
    private final String mailCloakingSeparator;

    private List<MessageState> acceptableMessageStates = ImmutableList.of(MessageState.BLOCKED, MessageState.HELD, MessageState.SENT);
    private JsonTransformer<ReplyTSProcessedMessageEventDTO> transformer = new JsonTransformer<>(ReplyTSProcessedMessageEventDTO.class, false);

    @Autowired
    public ActiveMQReporter(
            @Qualifier("dwJmsTemplate") JmsTemplate jmsTemplate,
            @Value("${mailcloaking.localized.buyer}") String buyerPrefix,
            @Value("${mailcloaking.localized.seller}") String sellerPrefix,
            @Value("${mailcloaking.seperator:.}") String mailCloakingSeparator // Do not ever change this, you will break replies to existing messages.
    ) {
        this.jmsTemplate = jmsTemplate;
        this.buyerPrefix = buyerPrefix;
        this.sellerPrefix = sellerPrefix;
        this.mailCloakingSeparator = mailCloakingSeparator;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        try (Timer.Context ignored = REPORTER_TIMER.time()) {
            if (shouldIgnoreMessage(message)) {
                return;
            }

            ReplyTSConversationDTO conversationDto = buildReplyTSConversationDTO(conversation);

            ReplyTSMessageDTO messageDto = buildReplyTSMessageDTO(conversation, message);

            ReplyTSProcessedMessageEventDTO replyTSProcessedMessageEventDTO = new ReplyTSProcessedMessageEventDTO(messageDto, conversationDto);
            try {
                jmsTemplate.convertAndSend(QUEUE_NAME, transformer.toJson(replyTSProcessedMessageEventDTO));
            } catch (JmsException e) {
                JMS_EXCEPTION_COUNTER.inc();
                throw e;
            }
        }
    }

    private ReplyTSMessageDTO buildReplyTSMessageDTO(Conversation conversation, Message message) {
        final MessageState messageState = message.getState();
        final boolean droppedDueToBadEmail = isMessageBlockedDueToBadEmailAddress(message);
        final boolean droppedDueToBannedUser = isMessageBlockedDueToBadUser(message);
        final Map<String, String> headers = message.getHeaders();
        final String senderIpAddress = headers.get(BoxHeaders.SENDER_IP_ADDRESS.getHeaderName());
        final DateTime sentDate = messageState == MessageState.SENT ? conversation.getLastModifiedAt() : null;
        final DateTime approvedDate = message.getHumanResultState() == ModerationResultState.GOOD ? conversation.getLastModifiedAt() : null;
        final DateTime rejectedDate = message.getHumanResultState() == ModerationResultState.BAD ? conversation.getLastModifiedAt() : null;
        final String httpHeaderIdHeader = headers.get(BoxHeaders.HTTP_HEADER_ID.getHeaderName());
        final String messageOrigin = headers.get(BoxHeaders.MESSAGE_ORIGIN.getHeaderName());

        return new ReplyTSMessageDTO(
                message.getId(),
                conversation.getId(),
                httpHeaderIdHeader,
                senderIpAddress,
                StringUtils.hasText(messageOrigin) ? messageOrigin : EMAIL_MESSAGE_ORIGIN,
                messageDirectionToType(message),
                messageStateToStatus(messageState),
                humanResultStateToModerationStatus(message.getHumanResultState()),
                isNotEmpty(message.getAttachmentFilenames()),
                droppedDueToBadEmail,
                droppedDueToBannedUser,
                sentDate,
                approvedDate,
                rejectedDate,
                message.getReceivedAt(),
                message.getLastModifiedAt()
        );
    }

    private ReplyTSConversationDTO buildReplyTSConversationDTO(Conversation conversation) {
        Map<String, String> originalMsgHeaders = conversation.getMessages().get(0).getHeaders();
        String replierIdHeader = originalMsgHeaders.get(BoxHeaders.REPLIER_ID.getHeaderName());
        String posterIdHeader = originalMsgHeaders.get(BoxHeaders.POSTER_ID.getHeaderName());
        String buyerAnonID = buyerPrefix + mailCloakingSeparator + conversation.getBuyerSecret();
        String sellerAnonID = sellerPrefix + mailCloakingSeparator + conversation.getSellerSecret();

        return new ReplyTSConversationDTO(
                conversation.getId(),
                Long.valueOf(conversation.getAdId()),
                conversation.getBuyerId(),
                buyerAnonID,
                originalMsgHeaders.get(BoxHeaders.REPLIER_NAME.getHeaderName()),
                StringUtils.hasText(replierIdHeader) ? Long.valueOf(replierIdHeader) : null,
                conversation.getSellerId(),
                sellerAnonID,
                StringUtils.hasText(posterIdHeader) ? Long.valueOf(posterIdHeader) : null,
                conversation.getCreatedAt(),
                conversation.getLastModifiedAt()
        );
    }

    private boolean shouldIgnoreMessage(Message message) {
        MessageState messageState = message.getState();
        if (!acceptableMessageStates.contains(messageState)) {
            return true;
        } else if (message.getMessageDirection() == MessageDirection.UNKNOWN) {
            return true;
        }

        return false;
    }

    /*
        Filter chain ending in "blocked" + user filter processing entry = bad email!
     */
    private boolean isMessageBlockedDueToBadEmailAddress(Message message) {
        boolean foundUserFilterResult = false;
        boolean foundFinalResult = false;

        for (ProcessingFeedback processingFeedback : message.getProcessingFeedback()) {
            String filterName = processingFeedback.getFilterName();
            if (UserfilterFactory.class.getCanonicalName().equals(filterName)) {
                foundUserFilterResult = true;
            } else if (FilterChain.class.getCanonicalName().equals(filterName) && FilterResultState.DROPPED.equals(message.getFilterResultState())) {
                foundFinalResult = true;
            }
        }

        return foundUserFilterResult && foundFinalResult;
    }

    /*
        Filter chain ending in "blocked" + either email-blocked-filter or ip-blocked-filter = bad user!
     */
    private boolean isMessageBlockedDueToBadUser(Message message) {
        boolean foundUserEmailFilterResult = false;
        boolean foundUserIpFilterResult = false;
        boolean foundFinalResult = false;

        for (ProcessingFeedback processingFeedback : message.getProcessingFeedback()) {
            String filterName = processingFeedback.getFilterName();
            if (EmailBlockedFilterFactory.class.getCanonicalName().equals(filterName)) {
                foundUserEmailFilterResult = true;
            } else if (IpBlockedFilterFactory.class.getCanonicalName().equals(filterName)) {
                foundUserIpFilterResult = true;
            } else if (FilterChain.class.getCanonicalName().equals(filterName) && FilterResultState.DROPPED.equals(message.getFilterResultState())) {
                foundFinalResult = true;
            }
        }

        return (foundUserEmailFilterResult || foundUserIpFilterResult) && foundFinalResult;
    }

    private ReplyTSMessageDTO.ModerationStatus humanResultStateToModerationStatus(ModerationResultState state) {
        switch (state) {
            case BAD:
                return ReplyTSMessageDTO.ModerationStatus.BAD;
            case GOOD:
                return ReplyTSMessageDTO.ModerationStatus.GOOD;
            case TIMED_OUT:
                return ReplyTSMessageDTO.ModerationStatus.TIMED_OUT;
            case UNCHECKED:
                return ReplyTSMessageDTO.ModerationStatus.UNCHECKED;
        }
        return null;
    }

    private ReplyTSMessageDTO.MessageStatus messageStateToStatus(MessageState state) {
        switch (state) {
            case SENT:
                return ReplyTSMessageDTO.MessageStatus.SENT;
            case HELD:
                return ReplyTSMessageDTO.MessageStatus.HELD;
            case BLOCKED:
                return ReplyTSMessageDTO.MessageStatus.DROPPED;
        }
        return null;
    }

    private ReplyTSMessageDTO.Type messageDirectionToType(Message message) {
        switch (message.getMessageDirection()) {
            case BUYER_TO_SELLER:
                return ReplyTSMessageDTO.Type.REPLIER_TO_POSTER;
            case SELLER_TO_BUYER:
                return ReplyTSMessageDTO.Type.POSTER_TO_REPLIER;
        }

        return null;
    }
}
