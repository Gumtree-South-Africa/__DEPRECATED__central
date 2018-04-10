package com.ecg.comaas.ebayk.filter.emailaddress;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;

class EmailAddressFilter implements Filter {

    private final EmailAddressFilterConfiguration config;

    EmailAddressFilter(EmailAddressFilterConfiguration config) {
        this.config = config;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {

        Set<String> matchedNumbers = new HashSet<>();

        findInTextParts(context, matchedNumbers);
        findInSubject(context, matchedNumbers);

        int score = config.getScore();

        return ImmutableList.copyOf(matchedNumbers.stream()
                .map(email -> new FilterFeedback(email, "Blocked email " + email, score, FilterResultState.HELD))
                .collect(toList()));
    }

    private void findInSubject(MessageProcessingContext context, Set<String> matchedNumbers) {
        String subject = Optional.ofNullable(context.getMail().get().getSubject()).orElse("");
        Set<String> matched = new EmailAddressMatcher(config.getBlockedEmailAddresses()).matches(subject);
        matchedNumbers.addAll(matched);
    }

    private void findInTextParts(MessageProcessingContext context, Set<String> matchedNumbers) {
        // read text parts instead of plainText parts. Include html link mailto:
        List<TypedContent<String>> plainTextParts = context.getMail().get().getTextParts(true);
        for (TypedContent<String> part : plainTextParts) {
            Set<String> matched = new EmailAddressMatcher(config.getBlockedEmailAddresses()).matches(part.getContent());
            matchedNumbers.addAll(matched);
        }
    }


}
