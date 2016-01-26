package com.ecg.replyts.core.runtime.persistence;

import com.basho.riak.client.builders.RiakObjectBuilder;
import com.google.common.base.Charsets;
import org.junit.Assert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GzipAwareContentFilterTest {

    public static final String REDUNDANT_CONTENT = "fffffoooofffffooooo";

    @Test
    public void passesNonGzipContentType() {

        String extract = new GzipAwareContentFilter(Charsets.UTF_8).readStringFromRiakObject(
                RiakObjectBuilder.newBuilder("foo", "bar")
                        .withContentType("application/text")
                        .withValue("foo".getBytes())
                        .build());
        Assert.assertEquals("foo", extract);
    }

    @Test
    public void compressesAndDecompressesGzippedContents() {

        byte[] compacted = GZip.zip(REDUNDANT_CONTENT.getBytes());

        String extract = new GzipAwareContentFilter(Charsets.UTF_8).readStringFromRiakObject(
                RiakObjectBuilder.newBuilder("foo", "bar")
                        .withContentType("application/x-gzip")
                        .withValue(compacted)
                        .build());
        assertThat(extract).isEqualTo(REDUNDANT_CONTENT);
    }
}
