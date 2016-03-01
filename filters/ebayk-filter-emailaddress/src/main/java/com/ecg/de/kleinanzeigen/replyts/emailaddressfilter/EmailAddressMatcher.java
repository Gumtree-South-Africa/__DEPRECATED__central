package com.ecg.de.kleinanzeigen.replyts.emailaddressfilter;

import com.google.common.base.CharMatcher;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.CharMatcher.inRange;
import static java.util.stream.Collectors.toSet;

public class EmailAddressMatcher {

    // Only consider lower case, we expect that the text is converted in lower case.
    private final CharMatcher ALPHA_NUM = inRange('a', 'z').or(inRange('0', '9'));

    private final Set<EmailAddress> blockedEmailAddresses;

    public EmailAddressMatcher(Set<EmailAddress> blockedEmailAddresses) {
        this.blockedEmailAddresses = blockedEmailAddresses;
    }

    public Set<String> matches(String text) {

        String normalized = new TextNormalizer().normalize(text);
        return blockedEmailAddresses.stream()
                .filter(candidate ->  normalized.contains(candidate.getComplete()) && isSeparatedWord(candidate, text))
                .map(EmailAddress::getComplete)
                .collect(toSet());
    }

    // Check, if found candidate is a separate word or only a partial match. Partial match would be a false positive.
    private boolean isSeparatedWord(EmailAddress candidate, String originalText) {
        Optional<Character> character = characterBeforeTerm(candidate.getNamePart(), originalText.toLowerCase());
        return !character.isPresent() || !ALPHA_NUM.matches(character.get());
    }

    private Optional<Character> characterBeforeTerm(String namePart, String originalText) {
        int index = originalText.indexOf(namePart);
        // Special case: namePart is first word in originalText (index == 0)
        // Special case: namePart won't match (index == -1). This might happen when name part was uglied and won't match in originalText
        if (index <= 0) {
            return Optional.empty();
        }
        return Optional.of(originalText.charAt(index - 1));
    }

}
