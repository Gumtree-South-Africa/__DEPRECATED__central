package com.ecg.replyts.core.runtime.mailparser;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.TextBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class PlaintextExtractingVisitorTest {

    @Mock
    private Entity e;
    @Mock
    private TextBody tb;

    private final PlaintextExtractingVisitor visitor = new PlaintextExtractingVisitor();

    @Before
    public void setUp() throws IOException {
        when(e.getDispositionType()).thenReturn("inline");
        when(e.getBody()).thenReturn(tb);

        when(tb.getReader()).thenReturn(new StringReader("<b>mail body</b>"));
    }

    @Test
    public void stripsHtml() throws Exception {
        when(e.getMimeType()).thenReturn("text/html");

        assertHtmlStripped();
    }

    @Test
    public void stripsXHtml() throws Exception {
        when(e.getMimeType()).thenReturn("application/xhtml+xml");

        assertHtmlStripped();
    }

    private void assertHtmlStripped() {
        visitor.visit(e, tb);
        List<String> contents = visitor.getContents();
        assertEquals(1, contents.size());
        assertEquals("mail body", contents.get(0));
    }

    @Test
    public void noHtmlStrippingWhenPlaintext() {
        when(e.getMimeType()).thenReturn("text/plain");
        visitor.visit(e, tb);
        List<String> contents = visitor.getContents();
        assertEquals(1, contents.size());
        assertEquals("<b>mail body</b>", contents.get(0));
    }

    @Test
    public void expectOrderedResultOnMutliTextParts() throws IOException {

        when(tb.getReader()).thenReturn(new StringReader("html"));
        when(e.getMimeType()).thenReturn("text/html");
        visitor.visit(e, tb);

        when(tb.getReader()).thenReturn(new StringReader("plain"));
        when(e.getMimeType()).thenReturn("text/plain");
        visitor.visit(e, tb);

        when(tb.getReader()).thenReturn(new StringReader("clean"));
        when(e.getMimeType()).thenReturn("text/clean");
        visitor.visit(e, tb);

        List<String> contents = visitor.getContents();

        assertEquals(3, contents.size());
        assertEquals("clean", contents.get(0));
        assertEquals("plain", contents.get(1));
        assertEquals("html", contents.get(2));
    }
}
