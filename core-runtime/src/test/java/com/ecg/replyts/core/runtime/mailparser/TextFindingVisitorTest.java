package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.TextBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TextFindingVisitorTest {

    @Mock
    private Entity e;
    @Mock
    private TextBody tb;
    @Mock
    private SingleBody otherBody;

    @Before
    public void setUp() throws Exception {
        when(e.getDispositionType()).thenReturn("inline");
        when(e.getBody()).thenReturn(tb);

        when(e.getMimeType()).thenReturn("text/plain");
        when(tb.getReader()).thenReturn(new StringReader("this is a mail body"));
    }

    @Test
    public void acceptsNormalTextBody() throws Exception {
        assertTrue(new TextFindingVisitor(true, true).accept(e));
    }

    @Test
    public void skipsAttachmentsWhenExcluded() throws Exception {
        when(e.getDispositionType()).thenReturn("attachment");
        when(e.getBody()).thenReturn(tb);

        assertFalse(new TextFindingVisitor(false, true).accept(e));
    }


    @Test
    public void skipsEntityIfNotTextBody() throws Exception {
        when(e.getDispositionType()).thenReturn("inline");
        when(e.getBody()).thenReturn(otherBody);

        assertFalse(new TextFindingVisitor(false, true).accept(e));
    }

    @Test
    public void acceptsAttachmentIfAllowed() throws Exception {
        when(e.getDispositionType()).thenReturn("attachment");
        when(e.getBody()).thenReturn(tb);

        assertTrue(new TextFindingVisitor(true, true).accept(e));
    }

    @Test
    public void extractsContentFromEntity() {
        TextFindingVisitor v = new TextFindingVisitor(true, true);
        v.visit(e, tb);

        List<TypedContent<String>> contents = v.getContents();

        assertEquals(1l, contents.size());

        assertEquals("this is a mail body", contents.get(0).getContent());
    }

    @Test(expected = IllegalStateException.class)
    public void forbidsModifyingContentsWhenImmutable() throws Exception {
        TextFindingVisitor v = new TextFindingVisitor(true, false);
        v.visit(e, tb);

        List<TypedContent<String>> contents = v.getContents();

        contents.get(0).overrideContent("oo");
    }

    @Test
    public void allowsOverridingContentsWhenMutable() throws Exception {
        // causes null pointer exception in modifying.
        when(e.removeBody()).thenReturn(tb);

        TextFindingVisitor v = new TextFindingVisitor(true, true);
        v.visit(e, tb);

        List<TypedContent<String>> contents = v.getContents();

        contents.get(0).overrideContent("oo");
    }
}
