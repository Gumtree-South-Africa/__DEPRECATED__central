package com.ecg.de.kleinanzeigen.replyts.phonenumberfilter;

import com.ecg.de.kleinanzeigen.replyts.phonenumberfilter.PhoneNumberFilterConfiguration.PhoneNumberConfiguration;
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

class PhoneNumberFilter implements Filter {

    private static final int MAX_SKIPPED_CHARACTERS = 10;
    private static final int MIN_GROUP_LENGTH = 11;

    private final PhoneNumberFilterConfiguration config;
    private final NumberStreamExtractor plainTextExtractor;
    private final HtmlLinkExtractor htmlLinkExtractor;

    PhoneNumberFilter(PhoneNumberFilterConfiguration config) {
        this(config, new NumberStreamExtractor(MAX_SKIPPED_CHARACTERS, MIN_GROUP_LENGTH), new HtmlLinkExtractor());
    }

    PhoneNumberFilter(PhoneNumberFilterConfiguration config, NumberStreamExtractor plainTextExtractor, HtmlLinkExtractor htmlLinkExtractor) {
        this.config = config;
        this.plainTextExtractor = plainTextExtractor;
        this.htmlLinkExtractor = htmlLinkExtractor;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {

        Set<PhoneNumberConfiguration> matchedNumbers = new HashSet<>();

        findInPlainTextParts(context, matchedNumbers);
        findInHtmlLinks(context, matchedNumbers);
        findInSubject(context, matchedNumbers);

        int score = config.getScore();

        return ImmutableList.copyOf(matchedNumbers.stream()
                .map(numberConfig -> new FilterFeedback(numberConfig.getNormalized(), "Blocked phone number " + numberConfig.getOriginal(), score, FilterResultState.HELD))
                .collect(toList()));
    }

    private void findInSubject(MessageProcessingContext context, Set<PhoneNumberConfiguration> matchedNumbers) {
        String subject = Optional.ofNullable(context.getMail().get().getSubject()).orElse("");
        NumberStream numberStream = plainTextExtractor.extractStream(subject);
        findInNumberStream(matchedNumbers, numberStream);
    }


    private void findInHtmlLinks(MessageProcessingContext context, Set<PhoneNumberConfiguration> matchedNumbers) {
        // Read text parts instead of plainTextParts() to find hidden phone numbers in html links: <a href="tel:0151..">
        List<TypedContent<String>> textParts = context.getMail().get().getTextParts(true);
        for (TypedContent<String> textPart : textParts) {
            NumberStream numberStream = htmlLinkExtractor.extractStream(textPart);
            findInNumberStream(matchedNumbers, numberStream);
        }
    }

    private void findInPlainTextParts(MessageProcessingContext context, Set<PhoneNumberConfiguration> matchedNumbers) {
        // true -> include text attachments. Some mails only contains text attachment as content.
        List<String> plainTextParts = context.getMail().get().getPlaintextParts();
        for (String part : plainTextParts) {
            NumberStream numberStream = plainTextExtractor.extractStream(part);
            findInNumberStream(matchedNumbers, numberStream);
        }
    }

    private void findInNumberStream(Set<PhoneNumberConfiguration> matchedNumbers, NumberStream numberStream) {
        for (PhoneNumberConfiguration config : this.config.getFraudulentPhoneNumbers()) {
            String numberCandidate = config.getNormalized();
            if (numberStream.contains(numberCandidate)) {
                matchedNumbers.add(config);
            }
        }
    }

}
