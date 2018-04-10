package com.ecg.comaas.ebayk.filter.phonenumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;


/**
 * Configuration Object for a phone number filter.
 */
class PhoneNumberFilterConfiguration {

    private static final String NUMBERS_FIELD = "numbers";
    private static final String SCORE_FIELD = "score";

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    private final Set<PhoneNumberConfiguration> fraudulentPhoneNumbers;
    private final int score;

    public static class PhoneNumberConfiguration {

        private final String original;
        private final String normalized;

        PhoneNumberConfiguration(String original, String normalized) {
            this.original = original;
            this.normalized = normalized;
        }

        /**
         * @return The normalized national part of the number
         */
        public String getNormalized() {
            return normalized;
        }

        /**
         * @return The configured number with international prefix.
         */
        public String getOriginal() {
            return original;
        }

        // Need equals/hashCode to put in Set to ignore duplicated configurations
        // only take the normalized number in account
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PhoneNumberConfiguration)) {
                return false;
            }
            PhoneNumberConfiguration conf = (PhoneNumberConfiguration) obj;
            return normalized.equals(conf.normalized);
        }

        @Override
        public int hashCode() {
            return normalized.hashCode();
        }
    }

    PhoneNumberFilterConfiguration(Set<PhoneNumberConfiguration> fraudulentPhoneNumbers, int score) {
        this.fraudulentPhoneNumbers = fraudulentPhoneNumbers;
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public Set<PhoneNumberConfiguration> getFraudulentPhoneNumbers() {
        return fraudulentPhoneNumbers;
    }


    public static PhoneNumberFilterConfiguration from(JsonNode configuration) {

        if (!configuration.hasNonNull(SCORE_FIELD)) {
            throw new RuntimeException(format("Missing int field '%s' in JSON configuration!", SCORE_FIELD));
        }
        if (!configuration.hasNonNull(NUMBERS_FIELD)) {
            throw new RuntimeException(format("Missing array field '%s' in JSON configuration!", NUMBERS_FIELD));
        }
        int score = configuration.get(SCORE_FIELD).asInt();
        ArrayNode rulesArray = (ArrayNode) configuration.get(NUMBERS_FIELD);
        Set<PhoneNumberConfiguration> configs = new HashSet<>();

        for (JsonNode jsonNode : rulesArray) {
            String originalNumber = readCleanedField(jsonNode);
            String normalizedNumber = parseNormalizedNationalPart(originalNumber);
            configs.add(new PhoneNumberConfiguration(originalNumber, normalizedNumber));
        }

        return new PhoneNumberFilterConfiguration(configs, score);
    }

    private static String parseNormalizedNationalPart(String number) {
        try {
            Phonenumber.PhoneNumber phoneNumber = PHONE_UTIL.parse(number, "DE");
            if (!PHONE_UTIL.isValidNumber(phoneNumber)) {
                throw new NumberFormatException("Not a valid phone number: " + number);
            }
            return Long.toString(phoneNumber.getNationalNumber());
        } catch (NumberParseException e) {
            throw new NumberFormatException(e.getMessage() + " value: " + number);
        }
    }

    private static String readCleanedField(JsonNode jsonNode) {
        return jsonNode.asText().replaceAll("[\\s-]", "");
    }
}
