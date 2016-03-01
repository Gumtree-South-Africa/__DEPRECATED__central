package com.ecg.de.kleinanzeigen.replyts.phonenumberfilter;

import com.ecg.de.kleinanzeigen.replyts.phonenumberfilter.PhoneNumberFilterConfiguration.PhoneNumberConfiguration;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.elasticsearch.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PhoneNumberFilterTest {

    private PhoneNumberFilter filter;

    @Mock
    private PhoneNumberFilterConfiguration config;

    @Mock
    private MessageProcessingContext context;

    @Mock
    private Mail mail;

    @Mock
    private NumberStreamExtractor extractor;

    @Mock
    private NumberStream stream;

    @Mock
    private HtmlLinkExtractor htmlLinkExtractor;

    @Before
    public void setUp() {
        filter = new PhoneNumberFilter(config, extractor, htmlLinkExtractor);
    }

    @Test
    public void ignoreDuplicateFindings() {
        when(config.getFraudulentPhoneNumbers()).thenReturn(ImmutableSet.of(new PhoneNumberConfiguration("0123", "123")));
        when(context.getMail()).thenReturn(mail);
        when(mail.getPlaintextParts()).thenReturn(asList("test 123 sdf", "sdfsf 123 gdfg"));

        when(extractor.extractStream(anyString())).thenReturn(stream);
        when(stream.contains("123")).thenReturn(true);

        List<FilterFeedback> feedback = filter.filter(context);

        assertThat(feedback).hasSize(1);
    }

    @Test
    public void findInSubject() {
        when(config.getFraudulentPhoneNumbers()).thenReturn(ImmutableSet.of(new PhoneNumberConfiguration("0123", "123")));
        when(context.getMail()).thenReturn(mail);
        when(mail.getSubject()).thenReturn("tel:123");

        when(extractor.extractStream("tel:123")).thenReturn(stream);
        when(stream.contains("123")).thenReturn(true);

        List<FilterFeedback> feedback = filter.filter(context);

        assertThat(feedback).hasSize(1);
    }

    @Test
    public void ignoreNullSubject() {
        when(config.getFraudulentPhoneNumbers()).thenReturn(ImmutableSet.of(new PhoneNumberConfiguration("0123", "123")));
        when(context.getMail()).thenReturn(mail);
        when(mail.getSubject()).thenReturn(null);
        when(extractor.extractStream(anyString())).thenReturn(stream);

        List<FilterFeedback> feedback = filter.filter(context);

        assertThat(feedback).isEmpty();
        verify(extractor, never()).extractStream(null);
    }

    @Test
    public void filterFeedbackContainsFilterConfigurationValues() {
        when(config.getFraudulentPhoneNumbers()).thenReturn(ImmutableSet.of(new PhoneNumberConfiguration("+491234567890", "1234567890")));
        when(config.getScore()).thenReturn(100);
        when(context.getMail()).thenReturn(mail);
        when(mail.getPlaintextParts()).thenReturn(singletonList("test 123 sdf"));

        when(extractor.extractStream(anyString())).thenReturn(stream);
        when(stream.contains("1234567890")).thenReturn(true);

        List<FilterFeedback> feedbackList = filter.filter(context);
        FilterFeedback feedback = feedbackList.get(0);

        assertThat(feedback.getDescription()).isEqualTo("Blocked phone number +491234567890");
        assertThat(feedback.getUiHint()).isEqualTo("1234567890");
        assertThat(feedback.getScore()).isEqualTo(100);
    }
}
