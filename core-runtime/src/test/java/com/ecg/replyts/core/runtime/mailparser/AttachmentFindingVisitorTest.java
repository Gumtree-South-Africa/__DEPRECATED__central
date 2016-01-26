package com.ecg.replyts.core.runtime.mailparser;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.SingleBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AttachmentFindingVisitorTest {
    @Mock
    private Entity e;

    @Mock
    private SingleBody b;

    private AttachmentFindingVisitor v = new AttachmentFindingVisitor();


    @Test
    public void detectsAttachmentViaContentDisposition() throws Exception {
        when(e.getDispositionType()).thenReturn("attachment");
        v.visit(e, b);
        assertTrue(v.hasAttachments());
    }

    @Test
    public void ignoresNotAttachmentParts() throws Exception {
        when(e.getDispositionType()).thenReturn("inline");
        v.visit(e, b);
        assertFalse(v.hasAttachments());
    }
}
