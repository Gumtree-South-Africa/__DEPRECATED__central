package com.ecg.comaas.ebayk.filter.emailaddress;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.mail.internet.AddressException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmailAddressFilterTest {

    private EmailAddressFilter filter;

    @Mock
    private EmailAddressFilterConfiguration config;

    @Mock
    private MessageProcessingContext context;

    @Mock
    private Mail mail;

    @Mock
    private TypedContent<String> part;

    @Before
    public void setUp() {
        filter = new EmailAddressFilter(config);
    }

    @Test
    public void ignoreDuplicateFindings() throws AddressException {
        when(config.getBlockedEmailAddresses()).thenReturn(addressList("foo@bar.com"));
        when(context.getMail()).thenReturn(Optional.of(mail));
        when(part.getContent()).thenReturn("foo@bar.com");
        when(mail.getTextParts(true)).thenReturn(asList(part, part));

        List<FilterFeedback> feedback = filter.filter(context);

        assertThat(feedback).hasSize(1);
    }

    private Set<EmailAddress> addressList(String address) throws AddressException {
        return ImmutableSet.of(new EmailAddress(address));
    }

    @Test
    public void findInSubject() throws AddressException {
        when(config.getBlockedEmailAddresses()).thenReturn(addressList("foo@bar.com"));
        when(context.getMail()).thenReturn(Optional.of(mail));
        when(part.getContent()).thenReturn("text");
        when(mail.getSubject()).thenReturn("My email: foo@bar.com");

        List<FilterFeedback> feedback = filter.filter(context);

        assertThat(feedback).hasSize(1);
    }

    @Test
    public void detectMixedCase() throws AddressException {
        when(config.getBlockedEmailAddresses()).thenReturn(addressList("marc.schnabel@freenet.de"));
        when(context.getMail()).thenReturn(Optional.of(mail));
        when(mail.getTextParts(true)).thenReturn(singletonList(part));
        when(part.getContent()).thenReturn("        Marc.Schnabel@freenet.de");
        when(mail.getSubject()).thenReturn("test");

        List<FilterFeedback> feedback = filter.filter(context);

        assertThat(feedback).hasSize(1);
    }

    @Test
    public void ignoreNullSubject() throws AddressException {
        when(config.getBlockedEmailAddresses()).thenReturn(addressList("foo@bar.com"));
        when(context.getMail()).thenReturn(Optional.of(mail));
        when(part.getContent()).thenReturn("text");
        when(mail.getSubject()).thenReturn(null);

        List<FilterFeedback> feedback = filter.filter(context);

        assertThat(feedback).isEmpty();
    }

    @Test
    public void matchBlockedEmailAddress() throws AddressException {
        when(config.getBlockedEmailAddresses()).thenReturn(addressList("foo@bar.com"));
        when(config.getScore()).thenReturn(100);
        when(context.getMail()).thenReturn(Optional.of(mail));
        when(mail.getTextParts(true)).thenReturn(singletonList(part));
        when(part.getContent()).thenReturn("foo@bar.com");

        List<FilterFeedback> feedbackList = filter.filter(context);
        FilterFeedback feedback = feedbackList.get(0);

        assertThat(feedback.getDescription()).isEqualTo("Blocked email foo@bar.com");
        assertThat(feedback.getUiHint()).isEqualTo("foo@bar.com");
        assertThat(feedback.getScore()).isEqualTo(100);
    }
}
