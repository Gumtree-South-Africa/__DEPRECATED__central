package com.ecg.replyts.core.runtime.mailparser;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.UnmodifiableIterator;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.stream.Field;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HeaderDecoderTest {

    private final List<Field> mailHeaders = new ArrayList<>();

    private Message mail;

    private final HeaderDecoder headerDecoder = new HeaderDecoder();

    @Before
    public void setUp() throws Exception {
        mail = mock(Message.class, Mockito.RETURNS_DEEP_STUBS);
        when(mail.getHeader().getFields()).thenReturn(mailHeaders);
    }

    @Test
    public void parsesToHeader() throws Exception {
        addField("To", "foo@bar.com");

        ImmutableMultimap<String, String> headers = headerDecoder.decodeHeaders(mail);
        assertThat(headers.get("To").size(), is(1));
        assertTrue(headers.containsEntry("To", "foo@bar.com"));
    }

    @Test
    public void parsesMultipleToHeadersWithCorrectOrder() throws Exception {
        addField("To", "foo@bar.com");
        addField("To", "foo2@bar.com");

        ImmutableMultimap<String, String> headers = headerDecoder.decodeHeaders(mail);
        assertThat(headers.get("To").size(), is(2));
        UnmodifiableIterator<String> toHeaderIt = headers.get("To").iterator();
        assertThat(toHeaderIt.next(), is("foo@bar.com"));
        assertThat(toHeaderIt.next(), is("foo2@bar.com"));
    }

    @Test
    public void normalizedHeaderNames() {
        addField("FOO-BAR", "as");
        ImmutableMultimap<String, String> headers = headerDecoder.decodeHeaders(mail);
        assertTrue(headers.containsEntry("Foo-Bar", "as"));
    }

    @Test
    public void doesNotNormalizeSystemHeaderNames() {
        addField("X-ADID", "123");
        addField("MIME-Version", "3");
        addField("Message-ID", "BAR");
        ImmutableMultimap<String, String> headers = headerDecoder.decodeHeaders(mail);
        assertTrue(headers.containsEntry("X-ADID", "123"));
        assertTrue(headers.containsEntry("MIME-Version", "3"));
        assertTrue(headers.containsEntry("Message-ID", "BAR"));
    }

    @Test
    public void normlizedSimpleHeaderCorrectly() {
        assertEquals("To", headerDecoder.normalizeHeaderName("TO"));
    }

    @Test
    public void normalizesMultiSegmentHeader() {
        assertEquals("Handle-With-Care", headerDecoder.normalizeHeaderName("HANDLE-with-CaRe"));
    }

    @Test
    public void capitalizedIdAtEnd() {
        assertEquals("Fo-ID", headerDecoder.normalizeHeaderName("fo-Id"));
    }

    @Test
    public void ignoresAdIdHeader() {
        assertEquals("X-ADID", headerDecoder.normalizeHeaderName("X-ADID"));
    }

    @Test
    public void testReplyTo() {
        addField("Reply-To", "bar@foo.com");
        ImmutableMultimap<String, String> headers = headerDecoder.decodeHeaders(mail);
        assertTrue(headers.containsEntry("Reply-To", "bar@foo.com"));
    }


    private void addField(String headerName, String headerValue) {
        Field mock = mock(Field.class);
        when(mock.getName()).thenReturn(headerName);
        when(mock.getBody()).thenReturn(headerValue);
        mailHeaders.add(mock);
    }
}
