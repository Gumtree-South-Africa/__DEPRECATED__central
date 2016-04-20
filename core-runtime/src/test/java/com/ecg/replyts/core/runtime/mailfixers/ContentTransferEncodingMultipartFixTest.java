package com.ecg.replyts.core.runtime.mailfixers;

import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.stream.Field;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ContentTransferEncodingMultipartFixTest {

    private Field f = mock(Field.class);

    private Message mail = mock(Message.class, Mockito.RETURNS_DEEP_STUBS);


    private ContentTransferEncodingMultipartFix fix = new ContentTransferEncodingMultipartFix();

    @Before
    public void setUp() throws Exception {
        when(mail.getHeader().getField("Content-Transfer-Encoding")).thenReturn(f);
        when(mail.isMultipart()).thenReturn(true);
        when(f.getBody()).thenReturn("foo");
    }

    @Test
    public void fixIgnoredIfHeaderMissing() throws Exception {
        when(mail.getHeader().getField(anyString())).thenReturn(null);
        fix.applyIfNecessary(mail);
        verifyHeaderNotTouched();
    }

    @Test
    public void fixIgnoredIfNotMultipart() throws Exception {
        when(mail.isMultipart()).thenReturn(false);
        fix.applyIfNecessary(mail);
        verifyHeaderNotTouched();
    }

    @Test
    public void fixIgnoredIfHeaderAllowed() throws Exception {
        when(f.getBody()).thenReturn("8bit");
        fix.applyIfNecessary(mail);
        verifyHeaderNotTouched();
    }

    @Test
    public void fixIfQuotedPrintable() {
        when(f.getBody()).thenReturn("Quoted-Printable");
        fix.applyIfNecessary(mail);
        verify(mail.getHeader()).removeFields("Content-Transfer-Encoding");
    }

    private void verifyHeaderNotTouched() {
        Header header = mail.getHeader();
        verify(header, never()).removeFields(anyString());
    }
}
