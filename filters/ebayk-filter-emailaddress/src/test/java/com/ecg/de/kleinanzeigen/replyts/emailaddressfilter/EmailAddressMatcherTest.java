package com.ecg.de.kleinanzeigen.replyts.emailaddressfilter;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import javax.mail.internet.AddressException;

import static org.assertj.core.api.Assertions.assertThat;

public class EmailAddressMatcherTest {

    private final EmailAddressMatcher matcher = new EmailAddressMatcher(ImmutableSet.of(new EmailAddress("foo@bar.de")));

    @Test
    public void doNotMatchPartsOfAddress() throws AddressException {
        assertThat(matcher.matches("Some text otherfoo@bar.de text.")).isEmpty();
    }

    @Test
    public void doNotMatchPartsOfAddressMixedCase() throws AddressException {
        assertThat(matcher.matches("Some text otherFoo@Bar.de text.")).isEmpty();
    }

    @Test
    public void matchAddressInText() throws AddressException {
        assertThat(matcher.matches("Some text foo@bar.de text.")).containsExactly("foo@bar.de");
    }

    @Test
    public void matchAddressMailTo() throws AddressException {
        assertThat(matcher.matches("Some text mailto:foo@bar.de text.")).containsExactly("foo@bar.de");
    }

    @Test
    public void matchMixedCase() throws AddressException {
        assertThat(matcher.matches("Some text Foo@Bar.de text.")).containsExactly("foo@bar.de");
    }

    @Test
    public void matchUglifiedAddress() throws AddressException {
        assertThat(matcher.matches("Some text foo(at)bar.de text.")).containsExactly("foo@bar.de");
    }

    @Test
    public void matchAddressStarting() throws AddressException {
        assertThat(matcher.matches("foo@bar.de text.")).containsExactly("foo@bar.de");
    }

    @Test
    public void matchAddressOnlyInText() throws AddressException {
        assertThat(matcher.matches("foo@bar.de")).containsExactly("foo@bar.de");
    }

    @Test
    public void matchAddressAfterCR() throws AddressException {
        assertThat(matcher.matches("\nfoo@bar.de")).containsExactly("foo@bar.de");
    }

}
