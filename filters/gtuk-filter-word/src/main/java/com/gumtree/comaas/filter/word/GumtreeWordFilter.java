package com.gumtree.comaas.filter.word;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableBiMap;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.Result;
import com.gumtree.filters.comaas.config.Rule;
import com.gumtree.filters.comaas.config.WordFilterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

public class GumtreeWordFilter implements com.ecg.replyts.core.api.pluginconfiguration.filter.Filter {
    private static final Logger LOG = LoggerFactory.getLogger(GumtreeWordFilter.class);

    private static final String KEY_STRIPPED_MAILS = GumtreeWordFilter.class.getName() + ":STRIPPED-MAILS";

    private Filter pluginConfig;

    private WordFilterConfig filterConfig;

    public GumtreeWordFilter(Filter pluginConfig, WordFilterConfig filterConfig) {
        this.pluginConfig = pluginConfig;
        this.filterConfig = filterConfig;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) throws ProcessingTimeExceededException {
        Set<Integer> categoryBreadCrumb = (Set<Integer>) context.getFilterContext().get("categoryBreadCrumb");

        if (hasExemptedCategory(filterConfig.getExemptedCategories(), categoryBreadCrumb)) {
            return Collections.emptyList();
        }

        List<String> textsToCheck = getTextsToCheck(context);
        if (textsToCheck.isEmpty()) {
            return Collections.emptyList();
        }

        return checkTexts(textsToCheck);
    }

    private List<FilterFeedback> checkTexts(List<String> textsToCheck) {
        List<FilterFeedback> reasons = new ArrayList<>();

        PatternBuilder patternBuilder = new PatternBuilder(filterConfig);
        Map<Rule, Pattern> compiledPatterns = patternBuilder.buildAndPrecompileFromWordFilterRules();

        compiledPatterns.forEach((rule, pattern) -> textsToCheck.stream()
          .map(part -> pattern.matcher(part))
          .filter(m -> m.find() && !rule.getExceptions().contains(m.group()))
          .findFirst().ifPresent(m -> {
              String description = longDescription(getClass(), "Matched: " + rule.getPattern());

              reasons.add(new FilterFeedback(m.group(), description, 0, resultFilterResultMap.get(filterConfig.getResult())));
        }));

        return reasons;
    }

    private List<String> getTextsToCheck(MessageProcessingContext context) {
        Object textsPlain = context.getFilterContext().get(KEY_STRIPPED_MAILS);

        if (textsPlain != null && textsPlain instanceof List<?>) {
            return (List<String>) textsPlain;
        } else {
            List<String> ptParts = new ArrayList<>();
            List<TypedContent<String>> contents = context.getMail().getTextParts(false);

            for (TypedContent<String> content : contents) {
                ptParts.add(content.getContent());
            }

            context.getFilterContext().put(KEY_STRIPPED_MAILS, ptParts);

            return ptParts;
        }
    }

    // Previously abstracted into shared code; decide if we want to duplicate or move into comaas-models

    static final String FILTER_NAME = "filterName";
    static final String FILTER_INSTANCE = "filterInstance";
    static final String FILTER_VERSION = "filterVersion";
    static final String DESCRIPTION = "description";

    static final ImmutableBiMap<Result, FilterResultState> resultFilterResultMap = new ImmutableBiMap.Builder()
      .put(Result.DROP, FilterResultState.DROPPED)
      .put(Result.HOLD, FilterResultState.HELD)
      .put(Result.STOP_FILTERING, FilterResultState.ACCEPT_AND_TERMINATE)
      .build();

    ObjectMapper objectMapper = new ObjectMapper();

    String longDescription(Class<?> clazz, String shortDescription) {
        ObjectNode objectNode = objectMapper.createObjectNode();

        objectNode.put(FILTER_NAME, clazz.getName());
        objectNode.put(FILTER_INSTANCE, pluginConfig.getInstanceId());
        objectNode.put(FILTER_VERSION, filterConfig.getVersion());
        objectNode.put(DESCRIPTION, shortDescription);

        try {
            return objectMapper.writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            LOG.error("Error converting processing feedback description as JSON", e);

            return "{ }";
        }
    }

    boolean hasExemptedCategory(List<Long> exemptedCategories, Set<Integer> breadCrumbs) {
        return breadCrumbs != null && exemptedCategories != null ? exemptedCategories.stream().anyMatch(id -> breadCrumbs.contains(id)) : false;
    }
}
