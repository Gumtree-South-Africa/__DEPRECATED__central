package com.ecg.replyts.core.api.webapi.model.imp;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import com.ecg.replyts.core.api.webapi.model.MessageRts;
import com.ecg.replyts.core.api.webapi.model.MessageRtsDirection;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.ecg.replyts.core.api.webapi.model.ProcessingFeedbackRts;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MessageRtsRest implements MessageRts, Serializable {

    private String id;

    private MessageRtsState state;

    private MessageRtsDirection messageDirection;

    private ConversationRts conversation;

    private FilterResultState filterResultState;

    private ModerationResultState humanResultState;

    private Date receivedDate;

    private List<ProcessingFeedbackRts> processingFeedback;

    private Map<String, String> mailHeaders;

    private String lastEditor;

    private String text;
    private List<String> attachments = Collections.emptyList();

    public MessageRtsRest() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageRtsState getState() {
        return state;
    }

    public void setState(MessageRtsState state) {
        this.state = state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageRtsDirection getMessageDirection() {
        return messageDirection;
    }

    public void setMessageDirection(MessageRtsDirection messageDirection) {
        this.messageDirection = messageDirection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConversationRts getConversation() {
        return conversation;
    }

    public void setConversation(ConversationRts conversation) {
        this.conversation = conversation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FilterResultState getFilterResultState() {
        return filterResultState;
    }

    public void setFilterResultState(FilterResultState filterResultState) {
        this.filterResultState = filterResultState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModerationResultState getHumanResultState() {
        return humanResultState;
    }

    public void setHumanResultState(ModerationResultState humanResultState) {
        this.humanResultState = humanResultState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ProcessingFeedbackRts> getProcessingFeedback() {
        return processingFeedback;
    }

    @Override
    public List<String> getAttachments() {
        return attachments;
    }

    public void setProcessingFeedback(List<ProcessingFeedbackRts> processingFeedback) {
        this.processingFeedback = processingFeedback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getMailHeaders() {
        return mailHeaders;
    }

    public void setMailHeaders(Map<String, String> mailHeaders) {
        this.mailHeaders = mailHeaders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLastEditor() {
        return lastEditor;
    }

    public void setAttachments(List<String> attachments) {
        if(attachments!=null) {
            this.attachments = attachments;
        }
    }

    public void setLastEditor(String lastEditor) {
        this.lastEditor = lastEditor;
    }
}
