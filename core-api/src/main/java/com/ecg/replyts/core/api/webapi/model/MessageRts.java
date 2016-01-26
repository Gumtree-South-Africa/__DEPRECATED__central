package com.ecg.replyts.core.api.webapi.model;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Gives all information about a Message that was sent via ReplyTS
 *
 * @author huttar
 */
public interface MessageRts {

    /**
     * @return message's unique identifier
     */
    String getId();

    /**
     * @return current processing state of the message
     */
    MessageRtsState getState();

    /**
     * @return the direction the message is aimed to (is the receiver the buyer or the seller)
     */
    MessageRtsDirection getMessageDirection();

    /**
     * @return conversation, this message belongs to
     */
    ConversationRts getConversation();

    /**
     * @return state this message was assigned to by normal filtering process
     */
    FilterResultState getFilterResultState();

    /**
     * @return state this message was assigned to by CS reps
     */
    ModerationResultState getHumanResultState();

    /**
     * @return date the message was received
     */
    Date getReceivedDate();

    /**
     * @return list of processing feedbacks for this message
     */
    List<ProcessingFeedbackRts> getProcessingFeedback();

    /**  @return a list of attachment filenames */
    List<String> getAttachments();

    /**
     * @return the E-Mail headers as a map. Please note that they do not necessarily need to be returned by any service.
     */
    Map<String, String> getMailHeaders();

    /**
     * @return the E-Mails text as plain text (without any HTML). Please note that the text does not necessarily need to
     * be returned by any service.
     */
    String getText();


}
