package com.ecg.de.kleinanzeigen.replyts.emailaddressfilter;

import com.ecg.replyts.core.api.util.JsonObjects;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class EmailAddressFilterConfigurationTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void parsesJsonCorrectly() {
        String json = "{\n" +
                "    'score': 100," +
                "    'values': ['test@blocked.mail.de']\n" +
                " }";
        EmailAddressFilterConfiguration config = EmailAddressFilterConfiguration.from(JsonObjects.parse(json));

        assertThat(config.getBlockedEmailAddresses())
                .containsExactly(new EmailAddress("test@blocked.mail.de"));

        assertThat(config.getScore()).isEqualTo(100);
    }

    @Test
    public void parsesMultipleEntries() {
        String json = "{\n" +
                "    'score': 100," +
                "    'values': ['test1@blocked.mail.de','test2@blocked.mail.de']\n" +
                " }";
        EmailAddressFilterConfiguration config = EmailAddressFilterConfiguration.from(JsonObjects.parse(json));

        assertThat(config.getBlockedEmailAddresses())
                .containsExactly(new EmailAddress("test1@blocked.mail.de"), new EmailAddress("test2@blocked.mail.de"));

        assertThat(config.getScore()).isEqualTo(100);
    }

    @Test
    public void stripWhiteSpaces() {
        String json = "{\n" +
                "    'score': 100," +
                "    'values': ['  test@blocked.mail.de  ']\n" +
                " }";
        EmailAddressFilterConfiguration config = EmailAddressFilterConfiguration.from(JsonObjects.parse(json));

        assertThat(config.getBlockedEmailAddresses())
                .containsExactly(new EmailAddress("test@blocked.mail.de"));
    }

    @Test
    public void readAddressesWithUnderscoreDash() {
        String json = "{\n" +
                "    'score': 100," +
                "    'values': ['foo-bar_foo@foo.com']\n" +
                " }";
        EmailAddressFilterConfiguration config = EmailAddressFilterConfiguration.from(JsonObjects.parse(json));

        assertThat(config.getBlockedEmailAddresses())
                .containsExactly(new EmailAddress("foo-bar_foo@foo.com"));
    }

    @Test
    public void rejectIfNotAnEmail() {

        assertThatThrownBy(() -> {
            String json = "{\n" +
                    "    'score': 100," +
                    "    'values': ['test.blocked.mail.de']\n" +
                    " }";
            EmailAddressFilterConfiguration.from(JsonObjects.parse(json));
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void rejectIfNoDomainPart() {

        assertThatThrownBy(() -> {
            String json = "{\n" +
                    "    'score': 100," +
                    "    'values': ['test@']\n" +
                    " }";
            EmailAddressFilterConfiguration.from(JsonObjects.parse(json));
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void rejectIfNoTLD() {

        assertThatThrownBy(() -> {
            String json = "{\n" +
                    "    'score': 100," +
                    "    'values': ['test@ebay']\n" +
                    " }";
            EmailAddressFilterConfiguration.from(JsonObjects.parse(json));
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void rejectIfNoNamePart() {

        assertThatThrownBy(() -> {
            String json = "{\n" +
                    "    'score': 100," +
                    "    'values': ['@ebay.de']\n" +
                    " }";
            EmailAddressFilterConfiguration.from(JsonObjects.parse(json));
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void ignoreDuplicateEntries() {
        String json = "{\n" +
                "    'score': 100," +
                "    'values': ['test@blocked.mail.de','test@blocked.mail.de']\n" +
                " }";
        EmailAddressFilterConfiguration config = EmailAddressFilterConfiguration.from(JsonObjects.parse(json));

        assertThat(config.getBlockedEmailAddresses()).hasSize(1);
        assertThat(config.getBlockedEmailAddresses())
                .containsExactly(new EmailAddress("test@blocked.mail.de"));
    }
}
