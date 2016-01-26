package com.ecg.replyts.core.runtime.persistence.conversation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConversationResumeIndexValueBuilderTest {

    @Test
    public void convertsCorrectly() {
        assertEquals("foo@bar.com|bar@foo.com|123", new ConversationResumeIndexValueBuilder("foo@bar.com", "bar@foo.com", "123").getIndexValue());
    }

    @Test
    public void convertsMailsToLowerCase() {
        assertEquals("foo@bar.com|bar@foo.com|123", new ConversationResumeIndexValueBuilder("FOO@BAR.COM", "BAR@FOO.COM", "123").getIndexValue());
    }

    @Test
    public void doesNotLowerCaseAdId() {
        assertEquals("foo@bar.com|bar@foo.com|FooBar", new ConversationResumeIndexValueBuilder("foo@bar.com", "bar@foo.com", "FooBar").getIndexValue());
    }
}
