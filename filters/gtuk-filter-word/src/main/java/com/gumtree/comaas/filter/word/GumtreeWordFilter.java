package com.gumtree.comaas.filter.word;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.Rule;
import com.gumtree.filters.comaas.config.WordFilterConfig;

import java.util.*;
import java.util.regex.Pattern;

import static com.gumtree.comaas.common.filter.GumtreeFilterUtil.hasExemptedCategory;
import static com.gumtree.comaas.common.filter.GumtreeFilterUtil.longDescription;
import static com.gumtree.comaas.common.filter.GumtreeFilterUtil.resultFilterResultMap;

public class GumtreeWordFilter implements com.ecg.replyts.core.api.pluginconfiguration.filter.Filter {
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
              String description = longDescription(getClass(), pluginConfig.getInstanceId(), filterConfig.getVersion(), "Matched: " + rule.getPattern());

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
}
