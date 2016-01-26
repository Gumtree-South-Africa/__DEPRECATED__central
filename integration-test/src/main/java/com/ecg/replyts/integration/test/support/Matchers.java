package com.ecg.replyts.integration.test.support;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

/**
 * RTS specific matchers.
 */
public final class Matchers {

    private static String mailCloakingDomain = "@test-platform.com";

    private Matchers() {
    }

    public static Matcher<? super String> isAnonymized() {
        return new TypeSafeMatcher<String>() {
            @Override
            public boolean matchesSafely(String address) {
                return address.endsWith(mailCloakingDomain);
            }

            public void describeTo(Description description) {
                description.appendText("an anonymous email address");
            }
        };
    }

    public static Matcher<? super Address> isAnonymizedAddress() {
        return new TypeSafeMatcher<Address>() {
            @Override
            public boolean matchesSafely(Address address) {
                return address instanceof InternetAddress &&
                        ((InternetAddress) address).getAddress().endsWith(mailCloakingDomain);
            }

            public void describeTo(Description description) {
                description.appendText("an anonymous email address");
            }
        };
    }
}
