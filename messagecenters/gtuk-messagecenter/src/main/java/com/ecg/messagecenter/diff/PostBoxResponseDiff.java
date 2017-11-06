package com.ecg.messagecenter.diff;

import com.codahale.metrics.Counter;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.collect.Sets;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static java.lang.Math.min;
import static java.util.Optional.ofNullable;

@Component
@ConditionalOnProperty(name = "webapi.diff.uk.enabled", havingValue = "true")
public class PostBoxResponseDiff {

    private static final int MAX_CHARS_TO_COMPARE = 215;
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final long ONE_MINUTE_IN_MILLIS = 60000;

    private final Counter pbRespDiffCounter = newCounter("diff.postBoxResponseDiff.counter");
    private final Counter convIdsInNewOnlyCounter = newCounter("diff.postBoxResponseDiff.convIdsInNewOnlyCounter");
    private final Counter convIdsInOldOnlyCounter = newCounter("diff.postBoxResponseDiff.convIdsInOldOnlyCounter");

    private final boolean checkUnreadCounts;
    private final boolean checkTextShortTrimmed;

    @Autowired
    public PostBoxResponseDiff(@Value("${messagebox.diff.checkUnreadCounts:true}") boolean checkUnreadCounts,
                               @Value("${messagebox.diff.checkTextShortTrimmed:true}") boolean checkTextShortTrimmed) {
        this.checkUnreadCounts = checkUnreadCounts;
        this.checkTextShortTrimmed = checkTextShortTrimmed;
    }

    public void diff(String userId, PostBox newValue, PostBoxDiff oldValue) {
        if (checkUnreadCounts) {
            int numUnreadConversations = newValue.getUnreadCounts().getNumUnreadConversations();
            if (numUnreadConversations != oldValue.postBoxResponse.getNumUnread()) {
                logDiffForPbResp(userId, "numUnread",
                        Integer.toString(numUnreadConversations),
                        Integer.toString(oldValue.postBoxResponse.getNumUnread()),
                        false);
            }
        }

        List<com.ecg.messagebox.model.ConversationThread> allNewConversations = newValue.getConversations();
        List<ConversationThread> allOldConversations = oldValue.postBox.getConversationThreads();
        List<PostBoxListItemResponse> allOldResponses = oldValue.postBoxResponse.getConversations();

        Set<String> newConvIds = allNewConversations.stream()
                .map(com.ecg.messagebox.model.ConversationThread::getId)
                .collect(Collectors.toSet());

        Set<String> oldConvIds = allOldConversations.stream()
                .map(AbstractConversationThread::getAdId)
                .collect(Collectors.toSet());

        Set<String> convIdsInNewOnly = Sets.difference(newConvIds, oldConvIds);
        Set<String> convIdsInOldOnly = Sets.difference(oldConvIds, newConvIds);

        if (convIdsInNewOnly.size() != convIdsInOldOnly.size()) {
            if (convIdsInNewOnly.size() > 0) {
                logPbConvDiff(
                        convIdsInNewOnlyCounter,
                        userId,
                        convIdsInNewOnly.size() + " conversations in new model only",
                        convIdsInNewOnly.stream().collect(Collectors.joining(", ")),
                        false
                );
            }
            if (convIdsInOldOnly.size() > 0) {
                logPbConvDiff(
                        convIdsInOldOnlyCounter,
                        userId,
                        convIdsInOldOnly.size() + " conversations in old model only",
                        convIdsInOldOnly.stream().collect(Collectors.joining(", ")),
                        false
                );
            }
        }

        if (convIdsInNewOnly.size() > 0 || convIdsInOldOnly.size() > 0) {
            Set<String> convIdsInBoth = Sets.intersection(oldConvIds, newConvIds);

            List<com.ecg.messagebox.model.ConversationThread> commonNewConvs = allNewConversations.stream()
                    .filter(c -> convIdsInBoth.contains(c.getId()))
                    .collect(Collectors.toList());

            List<ConversationThread> commonOldConvs = allOldConversations.stream()
                    .filter(c -> convIdsInBoth.contains(c.getConversationId()))
                    .collect(Collectors.toList());

            List<PostBoxListItemResponse> commonOldResponses = allOldResponses.stream()
                    .filter(c -> convIdsInBoth.contains(c.getId()))
                    .collect(Collectors.toList());

            checkPbConvResponses(userId, commonNewConvs, commonOldConvs, commonOldResponses);
        } else {
            checkPbConvResponses(userId, allNewConversations, allOldConversations, allOldResponses);
        }
    }

    private void checkPbConvResponses(String userId, List<com.ecg.messagebox.model.ConversationThread> newConvs, List<ConversationThread> oldConvs, List<PostBoxListItemResponse> oldResponses) {
        for (int i = 0; i < oldConvs.size(); i++) {
            com.ecg.messagebox.model.ConversationThread newConv = newConvs.get(i);
            ConversationThread oldConv = oldConvs.get(i);
            PostBoxListItemResponse oldResponse = oldResponses.get(i);

            BuyerSellerInfo bsInfo = new BuyerSellerInfo.BuyerSellerInfoBuilder(newConv.getParticipants()).build();

            String logConvPrefix = newConv.getLatestMessage().getType() + " - " + "conversations[" + newConv.getId() + "](" + i + ")";

            if (!newConv.getId().equals(oldConv.getConversationId())) {
                logDiffForPbResp(userId, logConvPrefix + ".id", newConv.getId(), oldConv.getConversationId(), false);
            } else {
                boolean useNewLogger = newConv.getMetadata().getCreationDate().isPresent();
                if (!ofNullable(bsInfo.getBuyerName()).orElse("").equals(oldConv.getBuyerName().orElse(null))) {
                    logDiffForPbResp(userId, logConvPrefix + ".buyerName", bsInfo.getBuyerName(), oldConv.getBuyerName().orElse(null), useNewLogger);
                }
                if (!ofNullable(bsInfo.getSellerName()).orElse("").equals(oldConv.getSellerName().orElse(null))) {
                    logDiffForPbResp(userId, logConvPrefix + ".sellerName", bsInfo.getSellerName(), oldConv.getSellerName().orElse(null), useNewLogger);
                }
                if (!bsInfo.getBuyerId().toString().equals(oldConv.getBuyerId().orElse(null))) {
                    logDiffForPbResp(userId, logConvPrefix + ".userIdBuyer", bsInfo.getBuyerId().toString(), oldConv.getBuyerId().orElse(null), useNewLogger);
                }
                if (!bsInfo.getSellerId().toString().equals(oldConv.getSellerId().orElse(null))) {
                    logDiffForPbResp(userId, logConvPrefix + ".userIdSeller", bsInfo.getSellerId().toString(), oldConv.getSellerId().orElse(null), useNewLogger);
                }
                if (!newConv.getAdId().equals(oldConv.getAdId())) {
                    logDiffForPbResp(userId, logConvPrefix + ".adId", newConv.getAdId(), oldConv.getAdId(), useNewLogger);
                }

                ConversationRole newConvRole = ConversationRoleUtil.getConversationRole(userId, newConv.getParticipants());
                ConversationRole oldConvRole = ConversationBoundnessFinder.lookupUsersRole(userId, oldConv);
                if (newConvRole != oldConvRole) {
                    logDiffForPbResp(userId, logConvPrefix + ".role", newConvRole.name(), oldConvRole.name(), useNewLogger);
                }
                if (checkUnreadCounts) {
                    boolean newContainsUnread = newConv.getNumUnreadMessages(userId) > 0;
                    boolean oldContainsUnread = oldConv.isContainsUnreadMessages();
                    if (newContainsUnread != oldContainsUnread) {
                        logDiffForPbResp(userId, logConvPrefix + ".numUnreadMessages", Boolean.toString(newContainsUnread), Boolean.toString(oldContainsUnread), useNewLogger);
                    }
                }

                MailTypeRts newBoundness = newConv.getLatestMessage().getSenderUserId().equals(userId) ? MailTypeRts.OUTBOUND : MailTypeRts.INBOUND;
                if (newBoundness != oldResponse.getBoundness()) {
                    logDiffForPbResp(userId, logConvPrefix + ".boundness", newBoundness.name(), oldResponse.getBoundness().name(), useNewLogger);
                }

                if (checkTextShortTrimmed) {
                    String newConvTextShortTrimmed = newConv.getLatestMessage().getText().substring(0, min(MAX_CHARS_TO_COMPARE, newConv.getLatestMessage().getText().length()));
                    String oldConvTextShortTrimmed = oldResponse.getTextShortTrimmed().substring(0, min(MAX_CHARS_TO_COMPARE, oldResponse.getTextShortTrimmed().length()));
                    logDiffForPbResp(userId, logConvPrefix + ".textShortTrimmed", newConvTextShortTrimmed, oldConvTextShortTrimmed, useNewLogger);
//                    if (!newConvTextShortTrimmed.equals(oldConvTextShortTrimmed)) {
//                         caters for data that was written without applying the updated regex patterns
//                        String cleanedOldMsg = MessagePreProcessor.removeEmailClientReplyFragment(
//                                oldConv.getBuyerName(),
//                                oldConv.getSellerName(),
//                                oldConvTextShortTrimmed,
//                                getMessageDirection(oldResponse.getRole(), oldResponse.getBoundness())
//                        );
//                        if (!newConvTextShortTrimmed.equals(cleanedOldMsg)) {
//                            logDiffForPbResp(userId, logConvPrefix + ".textShortTrimmed", newConvTextShortTrimmed, cleanedOldMsg, useNewLogger);
//                        }
//                    }
                }

                DateTimeFormatter formatter = DateTimeFormat.forPattern(DATE_FORMAT);
                long oldReceivedDateMillis = oldConv.getReceivedAt().getMillis();
                long newReceivedDateMillis = newConv.getLatestMessage().getReceivedDate().getMillis();
                if (Math.abs(oldReceivedDateMillis - newReceivedDateMillis) > ONE_MINUTE_IN_MILLIS) {
                    logDiffForPbResp(userId, logConvPrefix + ".receivedDate", newConv.getLatestMessage().getReceivedDate().toString(formatter),
                            oldConv.getReceivedAt().toString(formatter), useNewLogger);
                }
            }
        }
    }

    private void logPbConvDiff(Counter counter, String params, String name, String value, boolean useNewLogger) {
        counter.inc();
        DiffReporter.report(String.format("postBoxResponseDiff(%s) - %s - %s", params, name, value), useNewLogger);
    }

    private void logDiffForPbResp(String params, String fieldName, String newValue, String oldValue, boolean useNewLogger) {
        pbRespDiffCounter.inc();
        DiffReporter.report(
                String.format("postBoxResponseDiff(%s) - %s - new: '%s' vs old: '%s'", params, fieldName, newValue, oldValue),
                useNewLogger);
    }
}