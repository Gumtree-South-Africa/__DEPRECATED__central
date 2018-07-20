package com.ecg.comaas.bt.filter.identicalcap;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload.ConcernedUserRole;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload.ResultOrdering;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

public class IdenticalCapFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(IdenticalCapFilter.class);

    private SearchService searchService;

    private ConversationRepository conversationRepository;

    private FilterConfig filterConfig;

    public IdenticalCapFilter(SearchService searchService, ConversationRepository conversationRepository, FilterConfig config) {
        this.searchService = searchService;
        this.conversationRepository = conversationRepository;
        this.filterConfig = config;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) throws ProcessingTimeExceededException {
        Message message = messageProcessingContext.getMessage();

        ConversationRole fromRole = message.getMessageDirection().getFromRole();
        String senderMailAddress = messageProcessingContext.getConversation().getUserId(fromRole);

        String conversation_id = messageProcessingContext.getMail().get().getCustomHeaders().get("conversation_id") != null
          ? messageProcessingContext.getMail().get().getCustomHeaders().get("conversation_id") : null;

        if (conversation_id == null) {
            conversation_id = messageProcessingContext.getConversation().getCustomValues().get("conversation_id") != null
              ? messageProcessingContext.getConversation().getCustomValues().get("conversation_id") : null;
        }

        LOG.debug("Conversation id from the incoming message is {}", conversation_id);

        Set<String> categorySet = getInMsgCatTree(messageProcessingContext);

        // ignoring the follow up messages
        if (conversation_id != null) {
            LOG.debug("Ignoring follow-up from [{}]. Msg id: [{}]", senderMailAddress, message.getId());

            return Collections.emptyList();
        }

        // check for except categories list
        if (isExceptCategory(categorySet)) {
            LOG.debug("Ignoring the message from [{}] as the category belongs to the configured except categories list", senderMailAddress);

            return Collections.emptyList();
        }

        // either apply for all the categories or the list of configured categories
        if (isAllowedCategory(categorySet)) {
            RtsSearchResponse response = searchService.search(generateSearchPayLoad(senderMailAddress));

            String inMessage = trimToScanLength(message.getPlainTextBody().toLowerCase().replaceAll("[\\t\\n\\s]+", "").trim());

            LOG.debug("Incoming Message for the comparison is {}", inMessage);

            long matchesFound = response.getResult().stream()
              .map(this::retrieveMessageText)
              .flatMap(x -> x.entrySet().stream())
              .filter(e -> {
                  String eMessage = trimToScanLength(e.getValue().toLowerCase().replaceAll("[\\t\\n\\s]+", "").trim());

                  return eMessage.equals(inMessage) && !isExceptCategory(getRtsMsgCatTree(e.getKey()));
              })
              .peek(e -> LOG.debug("Matching Message found {}", e.getValue()))
              .limit(filterConfig.getMatchCount())
              .count();

            if (matchesFound == filterConfig.getMatchCount()) {
                LOG.debug("Scoring the message [{}] as it meets the rule condition", filterConfig.getScore());

                return Collections.singletonList(new FilterFeedback(getUiHint(), getDescription(), filterConfig.getScore(), FilterResultState.OK));
            }
        }

        return Collections.emptyList();
    }

    private Map<Conversation, String> retrieveMessageText(RtsSearchResponse.IDHolder id) {
        Conversation conversation = conversationRepository.getById(id.getConversationId());

        if (conversation == null) {
            LOG.warn("Skipping search result in API response; conversation not found: conversation={}", id.getConversationId());

            return Collections.emptyMap();
        }

        return Collections.singletonMap(conversation, conversation.getMessageById(id.getMessageId()).getPlainTextBody());
    }

    private String trimToScanLength(String message) {
        if (message.length() > filterConfig.getCharScanLength()) {
            return message.substring(0, filterConfig.getCharScanLength());
        } else {
            return message;
        }
    }

    private Set<String> getRtsMsgCatTree(Conversation conversation) {
        Set<String> result = new HashSet<>();

        if (conversation.getCustomValues().containsKey("categoryid")) {
            LOG.debug("Category id extracted from RTS Msg is {}", conversation.getCustomValues().get("categoryid"));

            result.add(conversation.getCustomValues().get("categoryid"));
        }
        if (conversation.getCustomValues().containsKey("l1-categoryid")) {
            LOG.debug("L1 Category id extracted from RTS Msg is {}", conversation.getCustomValues().get("l1-categoryid"));

            result.add(conversation.getCustomValues().get("l1-categoryid"));
        }
        if (conversation.getCustomValues().containsKey("l2-categoryid")) {
            LOG.debug("L2 Category id extracted from RTS Msg is {}", conversation.getCustomValues().get("l2-categoryid"));

            result.add(conversation.getCustomValues().get("l2-categoryid"));
        }
        if (conversation.getCustomValues().containsKey("l3-categoryid")) {
            LOG.debug("L3 category id extracted from RTS Msg is {}", conversation.getCustomValues().get("l3-categoryid"));

            result.add(conversation.getCustomValues().get("l3-categoryid"));
        }
        if (conversation.getCustomValues().containsKey("l4-categoryid")) {
            LOG.debug("L4 Category id extracted from RTS Msg is {}", conversation.getCustomValues().get("l4-categoryid"));

            result.add(conversation.getCustomValues().get("l4-categoryid"));
        }

        return result;
    }

    private Set<String> getInMsgCatTree(MessageProcessingContext messageProcessingContext) {
        String category_id = messageProcessingContext.getMail().get().getCustomHeaders().get("categoryid") != null
                ? messageProcessingContext.getMail().get().getCustomHeaders().get("categoryid") : null;

        if (category_id == null) {
            category_id = messageProcessingContext.getConversation().getCustomValues().get("categoryid") != null
                    ? messageProcessingContext.getConversation().getCustomValues().get("categoryid") : null;
        }

        String l1_category_id = messageProcessingContext.getMail().get().getCustomHeaders().get("l1-categoryid") != null
                ? messageProcessingContext.getMail().get().getCustomHeaders().get("l1-categoryid") : null;

        if (l1_category_id == null) {
            l1_category_id = messageProcessingContext.getConversation().getCustomValues().get("l1-categoryid") != null
                    ? messageProcessingContext.getConversation().getCustomValues().get("l1-categoryid") : null;
        }

        String l2_category_id = messageProcessingContext.getMail().get().getCustomHeaders().get("l2-categoryid") != null
                ? messageProcessingContext.getMail().get().getCustomHeaders().get("l2-categoryid") : null;

        if (l2_category_id == null) {
            l2_category_id = messageProcessingContext.getConversation().getCustomValues().get("l2-categoryid") != null
                    ? messageProcessingContext.getConversation().getCustomValues().get("categoryid") : null;
        }

        String l3_category_id = messageProcessingContext.getMail().get().getCustomHeaders().get("l3-categoryid") != null
                ? messageProcessingContext.getMail().get().getCustomHeaders().get("l3-categoryid") : null;

        if (l3_category_id == null) {
            l3_category_id = messageProcessingContext.getConversation().getCustomValues().get("l3-categoryid") != null
                    ? messageProcessingContext.getConversation().getCustomValues().get("categoryid") : null;
        }

        String l4_category_id = messageProcessingContext.getMail().get().getCustomHeaders().get("l4-categoryid") != null
                ? messageProcessingContext.getMail().get().getCustomHeaders().get("l4-categoryid") : null;

        if (l4_category_id == null) {
            l4_category_id = messageProcessingContext.getConversation().getCustomValues().get("l4-categoryid") != null
                    ? messageProcessingContext.getConversation().getCustomValues().get("l4-categoryid") : null;
        }

        Set<String> categorySet = new HashSet<>();

        if (StringUtils.hasText(category_id)) {
            LOG.debug("Category id extracted from incoming msg is  {}", category_id);

            categorySet.add(category_id);
        }

        if (StringUtils.hasText(l1_category_id)) {
            LOG.debug("L1 Category id extracted from incoming msg is {}", l1_category_id);

            categorySet.add(l1_category_id);
        }

        if (StringUtils.hasText(l2_category_id)) {
            LOG.debug("L2 Category id extracted from incoming msg is {}", l2_category_id);

            categorySet.add(l2_category_id);
        }

        if (StringUtils.hasText(l3_category_id)) {
            LOG.debug("L3 Category id extracted from incoming msg is {}", l3_category_id);

            categorySet.add(l3_category_id);
        }

        if (StringUtils.hasText(l4_category_id)) {
            LOG.debug("L4 Category id extracted from incoming msg is {}", l4_category_id);

            categorySet.add(l4_category_id);
        }

        return categorySet;
    }

    private boolean isAllowedCategory(Set<String> categorySet) {
        if (filterConfig.getCategories().isEmpty() || categorySet == null) {
            return true;
        }

        return categorySet.stream()
          .filter(c -> filterConfig.getCategories().contains(Integer.parseInt(c)))
          .peek(c -> LOG.debug("category id [{}] found in the allowed category list", c))
          .findFirst()
          .isPresent();
    }

    public boolean isExceptCategory(Set<String> categorySet) {
        if (filterConfig.getExceptCategories().isEmpty() || categorySet.isEmpty()) {
            return false;
        }

        return categorySet.stream()
          .filter(c -> filterConfig.getExceptCategories().contains(Integer.parseInt(c)))
          .peek(c -> LOG.debug("category id [{}] found in the except category list", c))
          .findFirst()
          .isPresent();
    }

    private SearchMessagePayload generateSearchPayLoad(String senderMailAddress) {
        SearchMessagePayload smp = new SearchMessagePayload();

        smp.setFromDate(getStartTime());
        smp.setToDate(DateTime.now().toDate());
        smp.setOffset(0);
        smp.setCount(25);
        smp.setOrdering(ResultOrdering.NEWEST_FIRST);
        smp.setUserRole(ConcernedUserRole.SENDER);
        smp.setUserEmail(senderMailAddress);

        return smp;
    }

    public Date getStartTime() {
        if (filterConfig.getLookupIntervalTimeUnit().equalsIgnoreCase("SECONDS")) {
            return DateTime.now().minusSeconds(filterConfig.getLookupInterval()).toDate();
        } else if (filterConfig.getLookupIntervalTimeUnit().equalsIgnoreCase("MINUTES")) {
            return DateTime.now().minusMinutes(filterConfig.getLookupInterval()).toDate();
        } else if (filterConfig.getLookupIntervalTimeUnit().equalsIgnoreCase("HOURS")) {
            return DateTime.now().minusHours(filterConfig.getLookupInterval()).toDate();
        } else {
            return DateTime.now().minusDays(filterConfig.getLookupInterval()).toDate();
        }
    }

    private String getUiHint() {
        return format("User sent more than %s similar emails in %s %s", filterConfig.getMatchCount(), filterConfig.getLookupInterval(), filterConfig.getLookupIntervalTimeUnit());
    }

    private String getDescription() {
        return format("User sent more than %s similar emails in %s %s", filterConfig.getMatchCount(), filterConfig.getLookupInterval(), filterConfig.getLookupIntervalTimeUnit());
    }
}