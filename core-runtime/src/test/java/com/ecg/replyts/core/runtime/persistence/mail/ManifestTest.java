package com.ecg.replyts.core.runtime.persistence.mail;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ManifestTest {

    @Test
    public void generatesManifest() {
        assertEquals("{\"chunks\":[\"foo1\",\"foo2\"]}", new Manifest(Lists.newArrayList("foo1", "foo2")).generate());
    }

    @Test
    public void parsesManifest() {
        assertEquals(Lists.newArrayList("foo1", "foo2"), Manifest.parse("{\"chunks\":[\"foo1\",\"foo2\"]}").getChunkKeys());

    }
}
