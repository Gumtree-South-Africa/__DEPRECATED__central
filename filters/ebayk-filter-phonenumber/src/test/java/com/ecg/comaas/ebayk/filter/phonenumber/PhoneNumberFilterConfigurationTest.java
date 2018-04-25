package com.ecg.comaas.ebayk.filter.phonenumber;

import com.ecg.replyts.core.api.util.JsonObjects;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class PhoneNumberFilterConfigurationTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void parsesJsonCorrectly() {
        String json = "{\n" +
                "    'score': 100," +
                "    'numbers': ['015156035123']\n" +
                " }";
        PhoneNumberFilterConfiguration config = PhoneNumberFilterConfiguration.from(JsonObjects.parse(json));


        assertEquals(1, config.getFraudulentPhoneNumbers().size());
        assertThat(config.getFraudulentPhoneNumbers())
                .extracting(PhoneNumberFilterConfiguration.PhoneNumberConfiguration::getNormalized)
                .containsExactly("15156035123");

        assertThat(config.getScore()).isEqualTo(100);
    }

    @Test
    public void removesSeperationCharactersFromConfig() {
        String json = "{\n" +
                "    'score': 100," +
                "    'numbers': ['0151 56 03 5123']\n" +
                " }";
        PhoneNumberFilterConfiguration config = PhoneNumberFilterConfiguration.from(JsonObjects.parse(json));

        assertThat(config.getFraudulentPhoneNumbers())
                .extracting(PhoneNumberFilterConfiguration.PhoneNumberConfiguration::getNormalized)
                .containsExactly("15156035123");
    }

    @Test
    public void readInternationalNumberWithPlus() {
        String json = "{\n" +
                "    'score': 100," +
                "    'numbers': ['+49 151 56 03 5123']\n" +
                " }";
        PhoneNumberFilterConfiguration config = PhoneNumberFilterConfiguration.from(JsonObjects.parse(json));

        assertThat(config.getFraudulentPhoneNumbers())
                .extracting(PhoneNumberFilterConfiguration.PhoneNumberConfiguration::getNormalized)
                .containsExactly("15156035123");
    }

    @Test
    public void readInternationalNumberWithZero() {
        String json = "{\n" +
                "    'score': 100," +
                "    'numbers': ['0049 151 56 03 5123']\n" +
                " }";
        PhoneNumberFilterConfiguration config = PhoneNumberFilterConfiguration.from(JsonObjects.parse(json));

        assertThat(config.getFraudulentPhoneNumbers())
                .extracting(PhoneNumberFilterConfiguration.PhoneNumberConfiguration::getNormalized)
                .containsExactly("15156035123");
    }

    @Test
    public void rejectIfNumberToShort() {

        assertThatThrownBy(() -> {
            String json = "{\n" +
                    "    'score': 100," +
                    "    'numbers': ['01515603512']\n" +
                    " }";
            PhoneNumberFilterConfiguration.from(JsonObjects.parse(json));
        }).isInstanceOf(NumberFormatException.class);
    }

    @Test
    public void ignoreDuplicateNumbers() {
        String json = "{\n" +
                "    'score': 100," +
                "    'numbers': ['015156035123','+4915156035123']\n" +
                " }";
        PhoneNumberFilterConfiguration config = PhoneNumberFilterConfiguration.from(JsonObjects.parse(json));

        assertThat(config.getFraudulentPhoneNumbers()).hasSize(1);
        assertThat(config.getFraudulentPhoneNumbers())
                .extracting(PhoneNumberFilterConfiguration.PhoneNumberConfiguration::getNormalized)
                .containsExactly("15156035123");
    }
}
