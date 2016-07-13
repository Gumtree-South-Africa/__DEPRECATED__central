package com.ecg.messagecenter.persistence;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.http.util.Constants;
import com.ecg.replyts.integration.riak.EmbeddedRiakClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class ConversationBlockRepositoryTest {
    public static final String CONV_ID = "convId";

    private IRiakClient riakClient = new EmbeddedRiakClient();
    private ConversationBlockRepository repository = new ConversationBlockRepository(riakClient);

    @Test
    public void persistsConversationBlock() throws Exception {
        Optional<DateTime> now = Optional.of(DateTime.now(DateTimeZone.UTC));
        ConversationBlock conversationBlock = new ConversationBlock(CONV_ID, 1, now, Optional.empty());
        repository.write(conversationBlock);

        IRiakObject fetchedObj = riakClient.fetchBucket(ConversationBlockRepository.BUCKET_NAME).execute().fetch(CONV_ID).execute();
        assertNotNull(fetchedObj);
        assertEquals(CONV_ID, fetchedObj.getKey());
        assertEquals(Constants.CTYPE_JSON_UTF8, fetchedObj.getContentType());
    }

    @Test
    public void cleansUpConversationBlocks() throws Exception {
        Optional<DateTime> now = Optional.of(DateTime.now(DateTimeZone.UTC));
        ConversationBlock conversationBlock = new ConversationBlock(CONV_ID, 1, now, Optional.empty());
        repository.write(conversationBlock);

        repository.cleanupOldConversationBlocks(now.get().plusSeconds(1));

        IRiakObject fetchedObj = riakClient.fetchBucket(ConversationBlockRepository.BUCKET_NAME).execute().fetch(CONV_ID).execute();

        assertNull(fetchedObj);
    }

    @Test
    public void doesntCleanUpRecentConversationBlocks() throws Exception {
        Optional<DateTime> now = Optional.of(DateTime.now(DateTimeZone.UTC));
        ConversationBlock conversationBlock = new ConversationBlock(CONV_ID, 1, now, Optional.empty());
        repository.write(conversationBlock);

        repository.cleanupOldConversationBlocks(now.get().minusSeconds(10));

        IRiakObject fetchedObj = riakClient.fetchBucket(ConversationBlockRepository.BUCKET_NAME).execute().fetch(CONV_ID).execute();

        assertNotNull(fetchedObj);
        assertEquals(CONV_ID, fetchedObj.getKey());
    }
}
