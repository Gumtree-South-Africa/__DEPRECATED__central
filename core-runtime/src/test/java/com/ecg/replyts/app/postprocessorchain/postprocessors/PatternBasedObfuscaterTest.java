package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.mailparser.StringTypedContentMime4J;
import com.google.common.net.MediaType;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PatternBasedObfuscaterTest {

    @Mock
    private MessageProcessingContext context;
    @Mock
    private MutableMail outgoingMail;
    private PatternBasedObfuscater instance;

    @Before
    public void setup() {
        when(context.getOutgoingMail()).thenReturn(outgoingMail);
        instance = new PatternBasedObfuscater();
    }


    @Test
    public void checkAllPatternsAreAppliedAndOverlappingHitsAreMerged() {
        //setup content
        List<TypedContent<String>> list = new ArrayList<TypedContent<String>>();
        String partString = "This part ain't got no hits in it";
        Entity mockEntity = new MockEntity();
        Body body = mock(Body.class);
        mockEntity.setBody(body);
        StringTypedContentMime4J part = new StringTypedContentMime4J(MediaType.PLAIN_TEXT_UTF_8, partString, mockEntity);
        list.add(part);
        when(outgoingMail.getTextParts(false)).thenReturn(list);
        //setup instance
        instance.addPattern(Pattern.compile("part.*got")); //overlaps with ...
        instance.addPattern(Pattern.compile("ain.*no")); //... this one
        instance.addPattern(Pattern.compile(" it")); //discrete

        instance.postProcess(context);

        assertThat(part.getContent(), is("This  hits in"));
    }

    @Test
    public void checkFallThruWithoutPatterns() {
        //setup content
        List<TypedContent<String>> list = new ArrayList<TypedContent<String>>();
        String partString = "This part ain't got no hits in it";
        Entity mockEntity = new MockEntity();
        Body body = mock(Body.class);
        mockEntity.setBody(body);
        StringTypedContentMime4J part = new StringTypedContentMime4J(MediaType.PLAIN_TEXT_UTF_8, partString, mockEntity);
        list.add(part);
        when(outgoingMail.getTextParts(false)).thenReturn(list);
        instance.postProcess(context);

        assertThat(part.getContent(), is(sameInstance(partString)));
    }

    /**
     * Performance considerations: Avoid instantiation of String objects if not needed
     */
    @Test
    public void checkPostProcessOnlyMassagesPartsWithHits() {
        List<TypedContent<String>> list = new ArrayList<TypedContent<String>>();
        String part1String = "This part ain't got no hits";
        Entity mockEntity1 = new MockEntity();
        Body body1 = mock(Body.class);
        mockEntity1.setBody(body1);
        String part2String = "This part got hits";
        Entity mockEntity2 = new MockEntity();
        Body body2 = mock(Body.class);
        mockEntity2.setBody(body2);
        TypedContent<String> part1 = new StringTypedContentMime4J(MediaType.PLAIN_TEXT_UTF_8, part1String, mockEntity1);
        list.add(part1);
        TypedContent<String> part2 = new StringTypedContentMime4J(MediaType.PLAIN_TEXT_UTF_8, part2String, mockEntity2);
        list.add(part2);
        when(outgoingMail.getTextParts(false)).thenReturn(list);

        instance.addPattern(Pattern.compile("got hits"));
        instance.postProcess(context);

        assertThat(list.get(0).getContent(), is(sameInstance(part1String)));
        assertThat(list.get(1).getContent(), is(not(sameInstance(part2String))));
    }

    @Test
    public void checkOrderIs1000() {
        assertThat(new PatternBasedObfuscater().getOrder(), is(1000));
    }

    private static class MockEntity implements Entity {

        private Body body;

        @Override
        public Entity getParent() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setParent(Entity parent) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Header getHeader() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setHeader(Header header) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Body getBody() {
            return body;
        }

        @Override
        public void setBody(Body body) {
            this.body = body;
        }

        @Override
        public Body removeBody() {
            Body old = body;
            body = null;
            return old;
        }

        @Override
        public boolean isMultipart() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getMimeType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getCharset() {
            return "UTF-8";
        }

        @Override
        public String getContentTransferEncoding() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getDispositionType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getFilename() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void dispose() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}