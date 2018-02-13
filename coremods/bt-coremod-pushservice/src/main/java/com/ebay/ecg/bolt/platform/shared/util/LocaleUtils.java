package com.ebay.ecg.bolt.platform.shared.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static java.lang.String.format;

public final class LocaleUtils {
    private static final Set<String> ISO_COUNTRIES = new HashSet<>(Arrays.asList(Locale.getISOCountries()));
    private static final Set<String> ISO_LANGUAGES = new HashSet<>(Arrays.asList(Locale.getISOLanguages()));

    public static final String validateISOCountryCode(String country) {
        Assert.notNull(country, "country ISO code cannot be null");
        Assert.isTrue(country.length() == 2, format("country ISO code '%s' is invalid. must be 2 upper case characters", country));

        if (!ISO_COUNTRIES.contains(country)) {
            throw new IllegalArgumentException(format("invalid ISO country code '%s'", country));
        }

        return country;
    }

    public static final String validateISOLanguageCode(String language) {
        Assert.notNull(language, "language ISO code cannot be null");
        Assert.isTrue(language.length() == 2, format("language ISO code '%s' is invalid, must be 2 upper case characters", language));

        if (!ISO_LANGUAGES.contains(language)) {
            throw new IllegalArgumentException(format("invalid ISO language code '%s'", language));
        }

        return language;
    }

    public static Locale parseLocale(String localeString) {
        if (localeString.contains("-")) {
            return parseLocaleFromHttpHeader(localeString);
        } else {
            return StringUtils.parseLocaleString(localeString);
        }
    }

    private static Locale parseLocaleFromHttpHeader(String localeString) {
        Assert.hasText(localeString, format("'%s' header value cannot be null or empty string", HttpHeaders.ACCEPT_LANGUAGE));

        String[] values = localeString.trim().split("-");

        if (values.length == 0 || values.length > 3) {
            throw new IllegalArgumentException(format("Illegal '%s' header locale value: {}", HttpHeaders.ACCEPT_LANGUAGE, localeString));
        }

        if (values.length == 1) {
            return new Locale(values[0]);
        } else if (values.length == 2) {
            return new Locale(values[0], values[1]);
        } else {
            return new Locale(values[0], values[1], values[2]);
        }
    }
}