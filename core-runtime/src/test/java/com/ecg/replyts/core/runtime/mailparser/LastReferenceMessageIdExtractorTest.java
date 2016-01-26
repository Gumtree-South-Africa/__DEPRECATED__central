package com.ecg.replyts.core.runtime.mailparser;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LastReferenceMessageIdExtractorTest {

    private static final String HEADER_VAL = "<" + new MessageIdHeaderEncryption().encrypt("5:123456") + "@test-domain.com>";

    private LastReferenceMessageIdExtractor extractor = new LastReferenceMessageIdExtractor();

    @Test
    public void testGetInReplyToMessageId_ReferencesHeaderIsPresent() throws Exception {
        org.apache.james.mime4j.dom.Message mail = mock(org.apache.james.mime4j.dom.Message.class);
        org.apache.james.mime4j.dom.Header header = mock(org.apache.james.mime4j.dom.Header.class);
        org.apache.james.mime4j.stream.Field referencesField = mock(org.apache.james.mime4j.stream.Field.class);
        when(mail.getHeader()).thenReturn(header);


        when(header.getFields()).thenReturn(Arrays.asList(referencesField));
        when(referencesField.getName()).thenReturn("References");
        when(referencesField.getBody()).thenReturn(HEADER_VAL);

        assertTrue(extractor.get(new StructuredMail(mail)).isPresent());
        assertThat(extractor.get(new StructuredMail(mail)).get(), is("5:123456"));
    }

    @Test
    public void testGetInReplyToMessageId_InReplyToHeaderIsPresent() throws Exception {
        org.apache.james.mime4j.dom.Message mail = mock(org.apache.james.mime4j.dom.Message.class);
        org.apache.james.mime4j.dom.Header header = mock(org.apache.james.mime4j.dom.Header.class);
        org.apache.james.mime4j.stream.Field referencesField = mock(org.apache.james.mime4j.stream.Field.class);
        when(mail.getHeader()).thenReturn(header);
        when(header.getFields()).thenReturn(Arrays.asList(referencesField));
        when(referencesField.getName()).thenReturn("In-Reply-To");
        when(referencesField.getBody()).thenReturn(HEADER_VAL);

        assertTrue(extractor.get(new StructuredMail(mail)).isPresent());
        assertThat(extractor.get(new StructuredMail(mail)).get(), is("5:123456"));
    }

    @Test
    public void testGetInReplyToMessageId_ReferencesHeaderIsPresentAndContainsMultipleReferences() throws Exception {
        org.apache.james.mime4j.dom.Message mail = mock(org.apache.james.mime4j.dom.Message.class);
        org.apache.james.mime4j.dom.Header header = mock(org.apache.james.mime4j.dom.Header.class);
        org.apache.james.mime4j.stream.Field referencesField = mock(org.apache.james.mime4j.stream.Field.class);
        when(mail.getHeader()).thenReturn(header);
        when(header.getFields()).thenReturn(Arrays.asList(referencesField));
        when(referencesField.getName()).thenReturn("References");
        when(referencesField.getBody()).thenReturn("<hpy9sd8ghjshgp7sh@test-domain.com> " + HEADER_VAL);

        assertTrue(extractor.get(new StructuredMail(mail)).isPresent());
        assertThat(extractor.get(new StructuredMail(mail)).get(), is("5:123456"));
    }

    @Test
    public void testGetInReplyToMessageId_ReferencesHeaderIsNotPresent() throws Exception {
        org.apache.james.mime4j.dom.Message mail = mock(org.apache.james.mime4j.dom.Message.class);
        org.apache.james.mime4j.dom.Header header = mock(org.apache.james.mime4j.dom.Header.class);
        when(mail.getHeader()).thenReturn(header);
        when(header.getFields()).thenReturn(Collections.<org.apache.james.mime4j.stream.Field>emptyList());

        assertFalse(extractor.get(new StructuredMail(mail)).isPresent());
    }

    @Test
    public void testGetInReplyToMessageId_ReferencesHeaderIsIllegal() throws Exception {
        org.apache.james.mime4j.dom.Message mail = mock(org.apache.james.mime4j.dom.Message.class);
        org.apache.james.mime4j.dom.Header header = mock(org.apache.james.mime4j.dom.Header.class);
        org.apache.james.mime4j.stream.Field referencesField = mock(org.apache.james.mime4j.stream.Field.class);
        when(mail.getHeader()).thenReturn(header);
        when(header.getFields()).thenReturn(Arrays.asList(referencesField));
        when(referencesField.getName()).thenReturn("References");
        when(referencesField.getBody()).thenReturn("<1htuw009wafiuhs@test-domain.com>");

        assertFalse(extractor.get(new StructuredMail(mail)).isPresent());
    }
}
