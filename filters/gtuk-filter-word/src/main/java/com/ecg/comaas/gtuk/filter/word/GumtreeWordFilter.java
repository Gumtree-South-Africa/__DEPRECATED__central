package com.ecg.comaas.gtuk.filter.word;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.Rule;
import com.gumtree.filters.comaas.config.WordFilterConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil.hasExemptedCategory;
import static com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil.longDescription;
import static com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil.resultFilterResultMap;

public class GumtreeWordFilter implements com.ecg.replyts.core.api.pluginconfiguration.filter.Filter {
    private static final String KEY_STRIPPED_MAILS = GumtreeWordFilter.class.getName() + ":STRIPPED-MAILS";

    private final Filter pluginConfig;
    private final Map<Rule, Pattern> compiledPatterns;
    private final WordFilterConfig filterConfig;

    GumtreeWordFilter(Filter pluginConfig, WordFilterConfig filterConfig) {
        this.pluginConfig = pluginConfig;
        this.filterConfig = filterConfig;
        this.compiledPatterns = new PatternBuilder(filterConfig).buildAndPrecompileFromWordFilterRules();
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        if (hasExemptedCategory(filterConfig, context)) {
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

        for (Map.Entry<Rule, Pattern> entry : compiledPatterns.entrySet()) {
            Rule rule = entry.getKey();
            Pattern pattern = entry.getValue();
            textsToCheck.stream()
                    .map(pattern::matcher)
                    .filter(m -> {
                        List<String> wordExceptions = rule.getExceptions();
                        if (wordExceptions == null) {
                            return m.find();
                        }
                        return m.find() && !wordExceptions.contains(m.group());
                    })
                    .findFirst().ifPresent(m -> {
                String description = longDescription(getClass(), pluginConfig.getInstanceId(), filterConfig.getVersion(), "Matched: " + rule.getPattern());

                reasons.add(new FilterFeedback(m.group(), description, 0, resultFilterResultMap.get(filterConfig.getResult())));
            });
        }

        return reasons;
    }

    private List<String> getTextsToCheck(MessageProcessingContext context) {
        Object textsPlain = context.getFilterContext().get(KEY_STRIPPED_MAILS);

        if (textsPlain instanceof List<?>) {
            //noinspection unchecked
            return (List<String>) textsPlain;
        } else {
            List<String> ptParts = new ArrayList<>();
            List<TypedContent<String>> contents = context.getMail().get().getTextParts(false);

            for (TypedContent<String> content : contents) {
                ptParts.add(content.getContent());
            }

            context.getFilterContext().put(KEY_STRIPPED_MAILS, ptParts);

            return ptParts;
        }
    }
}
