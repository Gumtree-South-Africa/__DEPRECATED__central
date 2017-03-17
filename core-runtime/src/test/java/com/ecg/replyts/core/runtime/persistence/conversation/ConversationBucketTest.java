package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.query.StreamingOperation;
import com.basho.riak.client.query.indexes.FetchIndex;
import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConversationBucketTest {
    @Mock
    private Bucket bucket;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FetchIndex<Object> fetchIndex;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IRiakClient riakClient;

    private ConversationBucket conversationBucket;

    @Before
    public void setUp() throws Exception {
        when(riakClient.fetchBucket("conversation").execute()).thenReturn(bucket);
        when(riakClient.updateBucket(bucket).allowSiblings(true).lastWriteWins(false).execute()).thenReturn(bucket);

        conversationBucket = new ConversationBucket(riakClient, "conversation", true, false);
    }

    @SuppressWarnings("unchecked")
    @Test
    // Dupes in index can occur when there are siblings in Riak
    public void modifiedBefore_removesDuplicates() throws Exception {
        int maxResults = 10;
        StreamingOperation<IndexEntry> streamingOp = mock(StreamingOperation.class);
        when(bucket.fetchIndex(any())).thenReturn(fetchIndex);
        when(fetchIndex.from(0).maxResults(maxResults).to(any()).executeStreaming()).thenReturn(streamingOp);
        when(streamingOp.getAll()).thenReturn(ImmutableList.of(
                new IndexEntry("1", "conv1"),
                new IndexEntry("2", "conv2"),
                new IndexEntry("3", "conv1"),
                new IndexEntry("4", "conv3")
        ));
        Iterable<String> convIds = conversationBucket.modifiedBefore(DateTime.now(), maxResults);
        assertThat(convIds, Matchers.containsInAnyOrder("conv1", "conv2", "conv3"));
    }
}
