package com.ecg.replyts.core.api.webapi.commands.payloads;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SearchMessagePayload {
    public String getAttachments() {
        return attachments;
    }

    @ApiModelProperty(notes = "Searches inside attachment filenames and supports wildcards. e.g. searching for `*.pdf`" +
            " will return all messages with pdf files attached.")
    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    public enum ResultOrdering {
        OLDEST_FIRST,
        NEWEST_FIRST
    }

    /**
     * Describes the role of a special mail address. To be used in combination with the userEmail field.
     */
    public enum ConcernedUserRole {
         ANY, // mail address can be both: sender or receiver
         SENDER,
         SENDER_ANONYMOUS,
         RECEIVER,
         RECEIVER_ANONYMOUS;

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

    @ApiModelProperty(notes = "Searches all messages that are currently in one of these states: \n" +
            "SENT: Message was sent to the final recipient. \n" +
            "HELD: Message will not be sent out automatically, because it needs to be checked by human.\n" +
            "BLOCKED: Message was classified as spam/fraud and will not be sent. \n" +
            "IGNORED: Messages that are automated replies (out of office mails, mailbox full messages,...)\n" +
            "ORPHANED: Message was received but the conversation it refers to (or the random number embedded) does not exist\n" +
            "QUARANTINE: Message is in Quarantine (like HELD)")
    @JsonProperty("messageState")
    public void setMessageState(MessageRtsState messageState) {
        this.messageStates = Collections.singletonList(messageState);
    }

    public ModerationResultState getHumanResultState() {
        return humanResultState;
    }

    @ApiModelProperty(notes = "Searches all messages that were moderated by CS agents with the outcome:\n" +
            "OK: CS Agent checked the mail and declared it non fraud/spam. Mail was delivered to receiver\n" +
            "DROPPED: CS Agent checked the mail and declared it fraud/spam. Mail was put to messageState BLOCKED \n" +
            "UNCHECKED: message was actually not yet checked by a cs agent\n" +
            "TIMED_OUT: mail was not checked by any CS agent in a given time frame (not supported by ReplyTS yet)")
    public void setHumanResultState(ModerationResultState humanResultState) {
        this.humanResultState = humanResultState;
    }

    public Date getFromDate() {
        return fromDate;
    }

    @ApiModelProperty(notes = "Searches for all Mails received after a given date. See Date Formats below")
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    @ApiModelProperty(notes = "Searches for all Mails received before a given date. See Date Formats below")
    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getUserEmail() {
        return userEmail;
    }

    @ApiModelProperty(notes = "Searches all messages where the sender/receiver/both are equal to the given mail address. \n" +
            "To control the role of the mail address (only sender, only receiver,...) use the userRole param.")
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public ConcernedUserRole getUserRole() {
        return userRole;
    }

    @ApiModelProperty(notes = "can only be used in combination with userEmail and accepts these values:\n" +
            "SENDER: userEmail only refers to the sender of the message\n" +
            "RECEIVER: userEmail only refers to the receiver of the message\n" +
            "ANY: userEmail only refers to sender or receiver of the message")
    public void setUserRole(ConcernedUserRole userRole) {
        this.userRole = userRole;
    }

    public String getAdId() {
        return adId;
    }

    @ApiModelProperty(notes = "only show messages from conversations that belong to a specific ad id. (can be any string)")
    public void setAdId(String adId) {
        this.adId = adId;
    }

    public String getMessageTextKeywords() {
        return messageTextKeywords;
    }

    @ApiModelProperty(notes = "contains keywords for a fulltext search on the mail body.")
    public void setMessageTextKeywords(String messageTextKeywords) {
        this.messageTextKeywords = messageTextKeywords;
    }

    public Map<String, String> getConversationCustomValues() {
        return conversationCustomValues;
    }

    @ApiModelProperty(notes = "a JSON map with custom value key/value pairs. Conversations can have arbitary custom values. " +
            "(they are passed to ReplyTS as X-CUST-FOO headers in conversation starter mails). " +
            "Custom value keys are lower cased without the X-CUST- prefix.\n" +
            "Example for header X-CUST-CATEGORYID: 123: \n" +
            "{ categoryid: 123 }")
    public void setConversationCustomValues(Map<String, String> conversationCustomValues) {
        this.conversationCustomValues = conversationCustomValues;
    }

    public String getFilterName() {
        return filterName;
    }

    @ApiModelProperty(notes = "finds all mails where a specific filter type (fully qualified type name) fired on. " +
            "Acceptable values can be found in the processing feedback section of any message the API returns.")
    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getFilterInstance() {
        return filterInstance;
    }

    @ApiModelProperty(notes = "only accepted in combination with filterName. Filters can have multiple filter instances" +
            " running. identifies the actual instance that needs to have fired on a mail.")
    public void setFilterInstance(String filterInstance) {
        this.filterInstance = filterInstance;
    }

    public ResultOrdering getOrdering() {
        return ordering;
    }

    @ApiModelProperty(notes = "defines the ordering of the search result:\n" +
            "OLDEST_FIRST: older messages on top\n" +
            "NEWEST_FIRST: newer messages on top")
    public void setOrdering(ResultOrdering ordering) {
        this.ordering = ordering;
    }

    public int getCount() {
        return count;
    }

    @ApiModelProperty(notes = "maximum number of results to be returned.")
    public void setCount(int count) {
        this.count = count;
    }

    public int getOffset() {
        return offset;
    }

    @ApiModelProperty(notes = "pagination: number of items to skip in the result set.")
    public void setOffset(int offset) {
        this.offset = offset;
    }


    public String getLastEditor() {
        return lastEditor;
    }

    @ApiModelProperty(notes = "searches for all messages that were moderated by a specific agent (see moderating a message)")
    public void setLastEditor(String lastEditor) {
        this.lastEditor = lastEditor;
    }
}