package com.ecg.messagebox.diff;

import com.codahale.metrics.Counter;
import com.ecg.messagecenter.util.MessagePreProcessor;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.api.model.conversation.ConversationRole.Buyer;
import static com.ecg.replyts.core.api.model.conversation.ConversationRole.Seller;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static com.ecg.replyts.core.api.webapi.model.MailTypeRts.INBOUND;
import static com.ecg.replyts.core.api.webapi.model.MailTypeRts.OUTBOUND;
import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static java.lang.Math.min;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

@Component
public class PostBoxResponseDiff {

    private static final int MAX_CHARS_TO_COMPARE = 215;
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final long ONE_MINUTE_IN_MILLIS = 60000;

    private final Counter pbRespDiffCounter = newCounter("diff.postBoxResponseDiff.counter");
    private final Counter convIdsInNewOnlyCounter = newCounter("diff.postBoxResponseDiff.convIdsInNewOnlyCounter");
    private final Counter convIdsInOldOnlyCounter = newCounter("diff.postBoxResponseDiff.convIdsInOldOnlyCounter");

    private final DiffReporter reporter;

    private final boolean checkUnreadCounts;
    private final boolean checkTextShortTrimmed;

    @Autowired
    public PostBoxResponseDiff(DiffReporter reporter,
                               @Value("${messagebox.diff.checkUnreadCounts:true}") boolean checkUnreadCounts,
                               @Value("${messagebox.diff.checkTextShortTrimmed:true}") boolean checkTextShortTrimmed) {
        this.reporter = reporter;
        this.checkUnreadCounts = checkUnreadCounts;
        this.checkTextShortTrimmed = checkTextShortTrimmed;
    }

    public void diff(String userId, PostBoxResponse newValue, PostBoxResponse oldValue) {
        if (checkUnreadCounts) {
            if (newValue.getNumUnread() != oldValue.getNumUnread()) {
                logDiffForPbResp(userId, "numUnread",
                        Integer.toString(newValue.getNumUnread()),
                        Integer.toString(oldValue.getNumUnread()),
                        false);
            }
        }

        if (!newValue.get_meta().equals(oldValue.get_meta())) {
            logDiffForPbResp(userId, "_meta", newValue.get_meta().toString(), oldValue.get_meta().toString(), false);
        }

        List<PostBoxListItemResponse> allNewConversations = newValue.getConversations();
        List<PostBoxListItemResponse> allOldConversations = oldValue.getConversations();
        Set<String> newConvIds = allNewConversations.stream().map(PostBoxListItemResponse::getId).collect(toSet());
        Set<String> oldConvIds = allOldConversations.stream().map(PostBoxListItemResponse::getId).collect(toSet());

        Set<String> convIdsInNewOnly = Sets.difference(newConvIds, oldConvIds);
        if (convIdsInNewOnly.size() > 0) {
            logPbConvDiff(
                    convIdsInNewOnlyCounter,
                    userId,
                    convIdsInNewOnly.size() + " conversations in new model only",
                    convIdsInNewOnly.stream().collect(Collectors.joining(", ")),
                    false
            );
        }

        Set<String> convIdsInOldOnly = Sets.difference(oldConvIds, newConvIds);
        if (convIdsInOldOnly.size() > 0) {
            logPbConvDiff(
                    convIdsInOldOnlyCounter,
                    userId,
                    convIdsInOldOnly.size() + " conversations in old model only",
                    convIdsInOldOnly.stream().collect(Collectors.joining(", ")),
                    false
            );
        }

        if (convIdsInNewOnly.size() > 0 || convIdsInOldOnly.size() > 0) {
            Set<String> convIdsInBoth = Sets.intersection(oldConvIds, newConvIds);
            List<PostBoxListItemResponse> commonNewConvs = allNewConversations.stream()
                    .filter(c -> convIdsInBoth.contains(c.getId()))
                    .collect(Collectors.toList());
            List<PostBoxListItemResponse> commonOldConvs = allOldConversations.stream()
                    .filter(c -> convIdsInBoth.contains(c.getId()))
                    .collect(Collectors.toList());
            checkPbConvResponses(userId, commonNewConvs, commonOldConvs);
        } else {
            checkPbConvResponses(userId, allNewConversations, allOldConversations);
        }
    }

    private void checkPbConvResponses(String userId, List<PostBoxListItemResponse> newConvs, List<PostBoxListItemResponse> oldConvs) {
        for (int i = 0; i < oldConvs.size(); i++) {
            PostBoxListItemResponse oldConv = oldConvs.get(i);
            PostBoxListItemResponse newConv = newConvs.get(i);

            String logConvPrefix = newConv.getMessageType() + " - " + "conversations[" + newConv.getId() + "](" + i + ")";

            if (!newConv.getId().equals(oldConv.getId())) {
                logDiffForPbResp(userId, logConvPrefix + ".id", newConv.getId(), oldConv.getId(), false);
            } else {
                boolean useNewLogger = newConv.getCreationDate() != null;
                if (!ofNullable(newConv.getBuyerName()).orElse("").equals(oldConv.getBuyerName())) {
                    logDiffForPbResp(userId, logConvPrefix + ".buyerName", newConv.getBuyerName(), oldConv.getBuyerName(), useNewLogger);
                }
                if (!ofNullable(newConv.getSellerName()).orElse("").equals(oldConv.getSellerName())) {
                    logDiffForPbResp(userId, logConvPrefix + ".sellerName", newConv.getSellerName(), oldConv.getSellerName(), useNewLogger);
                }
                if (!newConv.getUserIdBuyer().equals(oldConv.getUserIdBuyer())) {
                    logDiffForPbResp(userId, logConvPrefix + ".userIdBuyer", newConv.getUserIdBuyer().toString(), oldConv.getUserIdBuyer().toString(), useNewLogger);
                }
                if (!newConv.getUserIdSeller().equals(oldConv.getUserIdSeller())) {
                    logDiffForPbResp(userId, logConvPrefix + ".userIdSeller", newConv.getUserIdSeller().toString(), oldConv.getUserIdSeller().toString(), useNewLogger);
                }
                if (!newConv.getAdId().equals(oldConv.getAdId())) {
                    logDiffForPbResp(userId, logConvPrefix + ".adId", newConv.getAdId(), oldConv.getAdId(), useNewLogger);
                }
                if (newConv.getRole() != oldConv.getRole()) {
                    logDiffForPbResp(userId, logConvPrefix + ".role", newConv.getRole().name(), oldConv.getRole().name(), useNewLogger);
                }
                if (checkUnreadCounts) {
                    if (newConv.getNumUnreadMessages() != oldConv.getNumUnreadMessages()) {
                        logDiffForPbResp(userId, logConvPrefix + ".numUnreadMessages", Integer.toString(newConv.getNumUnreadMessages()), Integer.toString(oldConv.getNumUnreadMessages()), useNewLogger);
                    }
                }
                // conversation's latest message
                if (newConv.getBoundness() != oldConv.getBoundness()) {
                    logDiffForPbResp(userId, logConvPrefix + ".boundness", newConv.getBoundness().name(), oldConv.getBoundness().name(), useNewLogger);
                }

                if (checkTextShortTrimmed) {
                    String newConvTextShortTrimmed = newConv.getTextShortTrimmed().substring(0, min(MAX_CHARS_TO_COMPARE, newConv.getTextShortTrimmed().length()));
                    String oldConvTextShortTrimmed = oldConv.getTextShortTrimmed().substring(0, min(MAX_CHARS_TO_COMPARE, oldConv.getTextShortTrimmed().length()));
                    if (!newConvTextShortTrimmed.equals(oldConvTextShortTrimmed)) {
                        // caters for data that was written without applying the updated regex patterns
                        String cleanedOldMsg = MessagePreProcessor.removeEmailClientReplyFragment(
                                oldConv.getBuyerName(),
                                oldConv.getSellerName(),
                                oldConvTextShortTrimmed,
                                getMessageDirection(oldConv.getRole(), oldConv.getBoundness())
                        );
                        if (!newConvTextShortTrimmed.equals(cleanedOldMsg)) {
                            logDiffForPbResp(userId, logConvPrefix + ".textShortTrimmed", newConvTextShortTrimmed, cleanedOldMsg, useNewLogger);
                        }
                    }
                }

                // comparing with a margin of 1 minute apart, due to different timestamps inserted in old and new model
                SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
                long oldReceivedDateMillis, newReceivedDateMillis;
                try {
                    oldReceivedDateMillis = format.parse(newConv.getReceivedDate()).getTime();
                    newReceivedDateMillis = format.parse(oldConv.getReceivedDate()).getTime();
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                if (Math.abs(oldReceivedDateMillis - newReceivedDateMillis) > ONE_MINUTE_IN_MILLIS) {
                    logDiffForPbResp(userId, logConvPrefix + ".receivedDate", newConv.getReceivedDate(), oldConv.getReceivedDate(), useNewLogger);
                }
            }
        }
    }

    private MessageDirection getMessageDirection(ConversationRole conversationRole, MailTypeRts mailTypeRts) {
        if (conversationRole == Buyer && mailTypeRts == OUTBOUND)
            return BUYER_TO_SELLER;
        if (conversationRole == Buyer && mailTypeRts == INBOUND)
            return SELLER_TO_BUYER;
        if (conversationRole == Seller && mailTypeRts == OUTBOUND)
            return SELLER_TO_BUYER;
        if (conversationRole == Seller && mailTypeRts == INBOUND)
            return BUYER_TO_SELLER;
        throw new IllegalStateException("Unknown conversation role and rts mail type combination");
    }

    private void logPbConvDiff(Counter counter, String params, String name, String value, boolean useNewLogger) {
        counter.inc();
        reporter.report(String.format("postBoxResponseDiff(%s) - %s - %s", params, name, value), useNewLogger);
    }

    private void logDiffForPbResp(String params, String fieldName, String newValue, String oldValue, boolean useNewLogger) {
        pbRespDiffCounter.inc();
        reporter.report(
                String.format("postBoxResponseDiff(%s) - %s - new: '%s' vs old: '%s'", params, fieldName, newValue, oldValue),
                useNewLogger
        );
    }
}