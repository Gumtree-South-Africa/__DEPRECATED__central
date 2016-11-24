package com.ecg.replyts.core.api.webapi.commands.payloads;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Describes all required fields for searching a message
 */
public class SearchMessagePayload {


    public String getAttachments() {
        return attachments;
    }

    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    /**
     * enum containing all supported ways of sorting the search result
     */
    public enum ResultOrdering {
        /**
         * Older mails will be displayed first (default)
         */
        OLDEST_FIRST, /**
         * Newer mails will be displayed first
         */
        NEWEST_FIRST
    }

    /**
     * enum describing the role of a special mail address. To be used in combination with the userEmail field.
     */
    public enum ConcernedUserRole {
        /**
         * mail address can be both: sender or receiver
         */ANY, /**
         * mail address is sender
         */SENDER, /**
         */SENDER_ANONYMOUS, /**
         * mail address is receiver
         */RECEIVER, /**
         * mail address is receiver
         */RECEIVER_ANONYMOUS;

        public static final ConcernedUserRole DEFAULT = ANY;
    }

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<MessageRtsState> messageStates;
    private ModerationResultState humanResultState;

    private Date fromDate;
    private Date toDate;

    private String userEmail;
    private ConcernedUserRole userRole = ConcernedUserRole.DEFAULT;

    private String adId;

    private String messageTextKeywords;

    private Map<String, String> conversationCustomValues = Collections.emptyMap();

    private String filterName;
    private String filterInstance;
    private String lastEditor;
    private String attachments;

    private ResultOrdering ordering = ResultOrdering.OLDEST_FIRST;
    private int count;
    private int offset;


    public List<MessageRtsState> getMessageStates() {
        return messageStates;
    }

    @JsonProperty("messageStates")
    public void setMessageStates(List<MessageRtsState> messageStates) {
        this.messageStates = messageStates;
    }

    @JsonProperty("messageState")
    public void setMessageState(MessageRtsState messageState) {
        this.messageStates = Collections.singletonList(messageState);
    }

    public ModerationResultState getHumanResultState() {
        return humanResultState;
    }

    public void setHumanResultState(ModerationResultState humanResultState) {
        this.humanResultState = humanResultState;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public ConcernedUserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(ConcernedUserRole userRole) {
        this.userRole = userRole;
    }

    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    public String getMessageTextKeywords() {
        return messageTextKeywords;
    }

    public void setMessageTextKeywords(String messageTextKeywords) {
        this.messageTextKeywords = messageTextKeywords;
    }

    public Map<String, String> getConversationCustomValues() {
        return conversationCustomValues;
    }

    public void setConversationCustomValues(Map<String, String> conversationCustomValues) {
        this.conversationCustomValues = conversationCustomValues;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getFilterInstance() {
        return filterInstance;
    }

    public void setFilterInstance(String filterInstance) {
        this.filterInstance = filterInstance;
    }

    public ResultOrdering getOrdering() {
        return ordering;
    }

    public void setOrdering(ResultOrdering ordering) {
        this.ordering = ordering;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }


    public String getLastEditor() {
        return lastEditor;
    }

    public void setLastEditor(String lastEditor) {
        this.lastEditor = lastEditor;
    }


}
