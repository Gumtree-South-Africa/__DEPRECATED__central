package com.ecg.de.mobile.replyts.badword;


import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import de.mobile.cs.filter.domain.BadwordDTO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.api.model.conversation.FilterResultState.HELD;

class BadwordFilter implements Filter {

    private final CsFilterServiceClient filterService;

    BadwordFilter(CsFilterServiceClient csFilterServiceEndpoint) {
        this.filterService = csFilterServiceEndpoint;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        final String textBody = messageProcessingContext.getMessage().getPlainTextBody();
        final List<BadwordDTO> badwords = filterService.filterSwearwords(textBody);
        if (!badwords.isEmpty()) {

            final StringBuilder descriptionBuilder = new StringBuilder();

            descriptionBuilder.append("text contains swearwords ");

            descriptionBuilder.append(badwords.stream()
                    .map(badword -> badword.getTerm())
                    .limit(10)
                    .collect(Collectors.joining(" ")));

            if (badwords.size() > 10) {
                descriptionBuilder.append("...");
            }

            return ImmutableList.of(new FilterFeedback("BadwordFilter", descriptionBuilder.toString(), 0, HELD));
        }
        return Collections.emptyList();
    }
}