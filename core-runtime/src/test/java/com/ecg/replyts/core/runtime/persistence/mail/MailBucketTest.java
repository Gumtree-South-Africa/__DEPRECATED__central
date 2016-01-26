package com.ecg.replyts.core.runtime.persistence.mail;

import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.ecg.replyts.core.api.util.StorageSize;
import com.ecg.replyts.core.api.util.StorageUnit;
import com.ecg.replyts.core.runtime.persistence.mail.Chunks.Chunk;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class MailBucketTest {

    private MailBucket mailBucket;

    @Mock
    private BucketOpDelegate bucketOpDelegate;


    @Before
    public void setUp() throws RiakRetryFailedException {
        mailBucket = new MailBucket(bucketOpDelegate, new StorageSize(5, StorageUnit.B));
    }

    @Test
    public void storesInSingleObject() {
        DateTime convCreatedAt = new DateTime();
        mailBucket.persistMail(convCreatedAt, "foo", new byte[]{1, 2, 3, 4});
        verify(bucketOpDelegate).storeMailAsSingleObject(convCreatedAt, "foo", new byte[]{1, 2, 3, 4});
    }

    @Test
    public void splitsUpIntoElements() {
        DateTime convCreatedAt = new DateTime();
        when(bucketOpDelegate.storeOneChunk(any(DateTime.class), anyString(), any(Chunk.class))).thenReturn("part1", "part2");
        mailBucket.persistMail(convCreatedAt, "foo", new byte[]{1, 2, 3, 4, 5, 6});


        verify(bucketOpDelegate).storeOneChunk(convCreatedAt, "foo", new Chunk(0, new byte[]{1, 2, 3, 4, 5}));
        verify(bucketOpDelegate).storeOneChunk(convCreatedAt, "foo", new Chunk(1, new byte[]{6}));
    }

    @Test
    public void writesManifest() {
        DateTime convCreatedAt = new DateTime();
        when(bucketOpDelegate.storeOneChunk(any(DateTime.class), anyString(), any(Chunk.class))).thenReturn("part1", "part2");
        mailBucket.persistMail(convCreatedAt, "foo", new byte[]{1, 2, 3, 4, 5, 6});

        verify(bucketOpDelegate).storeManifest(convCreatedAt, "foo", new Manifest(Lists.newArrayList("part1", "part2")));

    }

    @Test
    public void loadsSingleChunkMail() {
        when(bucketOpDelegate.fetchKey("foo")).thenReturn(
                RiakObjectBuilder
                        .newBuilder("foo", "foo")
                        .withContentType("replyts/mail-complete")
                        .withValue(new byte[]{1, 2, 3})
                        .build());

        Optional<byte[]> content = mailBucket.load("foo");
        verify(bucketOpDelegate, never()).fetchChunk(anyString());

        assertArrayEquals(new byte[]{1, 2, 3}, content.get());

    }

    @Test
    public void loadsSindleMailFromUnsetMimeType() {
        when(bucketOpDelegate.fetchKey("foo")).thenReturn(
                RiakObjectBuilder
                        .newBuilder("foo", "foo")
                        .withContentType("anything/else")
                        .withValue(new byte[]{1, 2, 3})
                        .build());
        Optional<byte[]> content = mailBucket.load("foo");
        verify(bucketOpDelegate, never()).fetchChunk(anyString());

        assertArrayEquals(new byte[]{1, 2, 3}, content.get());
    }

    @Test
    public void returnsEmptyOptionWhenKeyNotFound() {
        Optional<byte[]> content = mailBucket.load("foo");
        assertFalse(content.isPresent());
    }

    @Test
    public void assemblesFromChunk() {

        when(bucketOpDelegate.fetchKey("foo")).thenReturn(
                RiakObjectBuilder
                        .newBuilder("foo", "foo")
                        .withContentType("replyts/mail-manifest")
                        .withValue("{\"chunks\":[\"foo1\",\"foo2\"]}")
                        .build());

        when(bucketOpDelegate.fetchChunk("foo1")).thenReturn(new byte[]{9, 1, 9});
        when(bucketOpDelegate.fetchChunk("foo2")).thenReturn(new byte[]{6, 1});
        Optional<byte[]> content = mailBucket.load("foo");

        assertArrayEquals(new byte[]{9, 1, 9, 6, 1}, content.get());
    }

    @Test
    public void deletesMailIfStoredAsSingleChunk() {
        when(bucketOpDelegate.fetchKey("foo")).thenReturn(
                RiakObjectBuilder.newBuilder("foo", "foo")
                        .withContentType("any/thing")
                        .withValue("")
                        .build());

        mailBucket.delete("foo");

        verify(bucketOpDelegate).delete("foo");
    }

    @Test
    public void deletesMailIfChunked() {

        when(bucketOpDelegate.fetchKey("foo")).thenReturn(
                RiakObjectBuilder
                        .newBuilder("foo", "foo")
                        .withContentType("replyts/mail-manifest")
                        .withValue("{\"chunks\":[\"foo1\",\"foo2\"]}")
                        .build());


        mailBucket.delete("foo");

        verify(bucketOpDelegate).delete("foo1");
        verify(bucketOpDelegate).delete("foo2");
        verify(bucketOpDelegate).delete("foo");
    }
}
