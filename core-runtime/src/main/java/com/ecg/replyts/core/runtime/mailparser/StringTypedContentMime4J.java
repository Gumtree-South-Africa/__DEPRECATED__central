package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.google.common.net.MediaType;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.TextBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;


public class StringTypedContentMime4J extends TypedContent<String> {

    private Entity e;

    public class OverrideTextBody extends TextBody {

        private String charset;

        public OverrideTextBody(String charset, String newBody) {
            this.charset = charset;
            setContent(newBody);
        }

        @Override
        public String getMimeCharset() {
            return charset;
        }

        @Override
        public Reader getReader() throws IOException {
            return new StringReader(getContent());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(getContent().getBytes(this.charset));
        }

    }

    public StringTypedContentMime4J(MediaType t, String content, Entity e) {
        super(t, content);
        this.e = e;
    }

    @Override
    public boolean isMutable() {
        return e != null;
    }

    @Override
    public void overrideContent(String newContent) {
        if (!isMutable())
            throw new IllegalStateException("This Mail Body cannot be changed. The Mail is immutable");
        e.removeBody().dispose();
        e.setBody(new OverrideTextBody(e.getCharset(), newContent));
    }
}
