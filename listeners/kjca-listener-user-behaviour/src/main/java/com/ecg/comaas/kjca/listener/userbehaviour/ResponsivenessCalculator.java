package com.ecg.comaas.kjca.listener.userbehaviour;

import com.ecg.comaas.kjca.coremod.shared.BoxHeaders;
import com.ecg.comaas.kjca.listener.userbehaviour.model.ResponsivenessRecord;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.ListIterator;

/**
 * Calculates user responsiveness within a conversation.
 * For details, see https://ecgwiki.corp.ebay.com/x/ehn-Bw (page id 134158714)
 */
@Component
@ConditionalOnBean(UserResponsivenessListener.class)
public class ResponsivenessCalculator {
    static final int VERSION = 1; // Format version. We may add/change fields in the future.

    /**
     * Generate a user "responsiveness record" based on the given conversation and message.
     * <p>
     * Basic idea:
     * <ol>
     * <li>Only operate on sent messages.</li>
     * <li>
     * Walk backwards from latest message to find the one we're working on.
     * This needs to be done because the current message may have just been sent after being delayed, and other
     * (later) messages may have been sent immediately.
     * </li>
     * <li>Then continue walking backwards until we either find our own or the other party's SENT message</li>
     * <li>If we find our own message, don't return a record, because we're just replying to ourselves.</li>
     * <li>
     * If we find a non-SENT message of the other party, don't return a record, because the current party
     * can't be replying to something they haven't seen.
     * </li>
     * </ol>
     *
     * @param conversation Complete conversation record
     * @param message      Message that is the basis for the calculation. The record will be generated
     *                     for the user that sent it.
     * @return The record if responsiveness can be calculated. Otherwise, null.
     * @see <a href="https://ecgwiki.corp.ebay.com/x/ehn-Bw">the wiki page</a> for details.
     */
    public ResponsivenessRecord calculateResponsiveness(Conversation conversation, Message message) {
        if (!basicChecksPass(conversation, message)) {
            return null;
        }

        List<Message> messages = conversation.getMessages();

        String userIdHeader = getUserIdHeader(messages.get(0), message.getMessageDirection());
        if (userIdHeader == null) {
            return null;
        }

        Message messageBeingRespondedTo = findMessageBeingRespondedTo(message, messages);
        if (messageBeingRespondedTo == null) {
            return null;
        }

        DateTime startOfDelta = messageBeingRespondedTo.getLastModifiedAt();
        DateTime endOfDelta = message.getReceivedAt();
        int secondsSinceLastResponse = Seconds.secondsBetween(startOfDelta, endOfDelta).getSeconds();

        return new ResponsivenessRecord(
                VERSION,
                Long.valueOf(userIdHeader),
                conversation.getId(),
                message.getId(),
                secondsSinceLastResponse,
                Instant.now()
        );
    }

    private boolean basicChecksPass(Conversation conversation, Message message) {
        return message.getState().equals(MessageState.SENT) && conversation.getMessages().size() < 500;
    }

    private String getUserIdHeader(Message firstMessageInConversation, MessageDirection directionOfCurrentMessage) {
        String headerNameForUserId = directionOfCurrentMessage == MessageDirection.BUYER_TO_SELLER
                ? BoxHeaders.REPLIER_ID.getHeaderName()
                : BoxHeaders.POSTER_ID.getHeaderName();
        return firstMessageInConversation.getCaseInsensitiveHeaders().get(headerNameForUserId);
    }

    private Message findMessageBeingRespondedTo(Message ourMessage, List<Message> messages) {
        ListIterator<Message> iterator = messages.listIterator(messages.size());

        // Find our message
        while (iterator.hasPrevious()) {
            if (iterator.previous().getId().equals(ourMessage.getId())) {
                break;
            }
        }

        // Looking back in time from our message, find the other party's latest sent message
        // If we find our own message first, back out. We don't want to count responsiveness
        // more than once per back-and-forth exchange.
        while (iterator.hasPrevious()) {
            Message prevMessage = iterator.previous();
            if (prevMessage.getMessageDirection().equals(ourMessage.getMessageDirection())) {
                return null;
            } else if (ourMsgCouldBeAReplyToPrevMsg(ourMessage, prevMessage)) {
                return prevMessage;
            }
        }

        return null;
    }

    private boolean ourMsgCouldBeAReplyToPrevMsg(Message ourMessage, Message prevMessage) {
        return prevMessage.getState().equals(MessageState.SENT)
                && prevMessage.getLastModifiedAt().isBefore(ourMessage.getReceivedAt());
    }
}