package com.ecg.replyts.core.runtime.mailparser;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.TextBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TextBodyCharsetValidatingVisitorTest {
    @Mock
    private TextBody bdy;

    @Mock
    private Entity entity;

    private TextBodyCharsetValidatingVisitor visitor = new TextBodyCharsetValidatingVisitor();

    @Before
    public void setUp() {
        when(entity.getBody()).thenReturn(bdy);
    }

    @Test
    public void recognizesExceptionWhenUnsupportedBody() throws IOException {
        when(bdy.getReader()).thenThrow(new UnsupportedEncodingException());

        visitor.visit(entity, bdy);

        assertTrue(visitor.getEncodingErrors().isPresent());
    }

    @Test
    public void worksWellOnFineBodies() {
        visitor.visit(entity, bdy);

        assertFalse(visitor.getEncodingErrors().isPresent());
    }
}
