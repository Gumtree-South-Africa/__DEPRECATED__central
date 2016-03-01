package com.ecg.de.kleinanzeigen.replyts.emailaddressfilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;


/**
 * Configuration Object for a email address filter.
 */
class EmailAddressFilterConfiguration {

    private static final String EMAIL_FIELD = "values";
    private static final String SCORE_FIELD = "score";

    private final Set<EmailAddress> blockedEmailAddresses;
    private final int score;

    EmailAddressFilterConfiguration(Set<EmailAddress> blockedEmailAddresses, int score) {
        this.blockedEmailAddresses = blockedEmailAddresses;
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public Set<EmailAddress> getBlockedEmailAddresses() {
        return blockedEmailAddresses;
    }

    public static EmailAddressFilterConfiguration from(JsonNode configuration) {

        if (!configuration.hasNonNull(SCORE_FIELD)) {
            throw new RuntimeException(format("Missing int field '%s' in JSON configuration!", SCORE_FIELD));
        }
        if (!configuration.hasNonNull(EMAIL_FIELD)) {
            throw new RuntimeException(format("Missing array field '%s' in JSON configuration!", EMAIL_FIELD));
        }
        int score = configuration.get(SCORE_FIELD).asInt();
        ArrayNode rulesArray = (ArrayNode) configuration.get(EMAIL_FIELD);
        Set<EmailAddress> configs = new HashSet<>();

        for (JsonNode jsonNode : rulesArray) {
            String emailAddressString = readCleanedField(jsonNode);
            EmailAddress emailAddress =  new EmailAddress(emailAddressString);
            configs.add(emailAddress);
        }
        return new EmailAddressFilterConfiguration(configs, score);
    }


    private static String readCleanedField(JsonNode jsonNode) {
        return jsonNode.asText().trim().toLowerCase();
    }

}
