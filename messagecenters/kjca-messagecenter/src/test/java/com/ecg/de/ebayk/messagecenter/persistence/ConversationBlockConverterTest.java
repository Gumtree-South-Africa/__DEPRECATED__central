package com.ecg.de.ebayk.messagecenter.persistence;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.cap.BasicVClock;
import com.basho.riak.client.cap.VClock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class ConversationBlockConverterTest {
    private ConversationBlockConverter converter;
    private VClock vclock;

    @Before
    public void setUp() throws Exception {
        converter = new ConversationBlockConverter(ConversationBlockRepository.BUCKET_NAME);
        vclock = new BasicVClock(new byte[] {});
    }

    @Test
    public void allFieldsSet() throws Exception {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        ConversationBlock originalConvoBlock = new ConversationBlock("convid", 1, Optional.of(now), Optional.of(now));

        IRiakObject riakObject = converter.fromDomain(originalConvoBlock, vclock);
        ConversationBlock extractedConvoBlock = converter.toDomain(riakObject);

        assertEquals(originalConvoBlock.getBuyerBlockedSellerAt(), extractedConvoBlock.getBuyerBlockedSellerAt());
        assertEquals(originalConvoBlock.getSellerBlockedBuyerAt(), extractedConvoBlock.getSellerBlockedBuyerAt());
        assertEquals(originalConvoBlock.getVersion(), extractedConvoBlock.getVersion());
        assertEquals(originalConvoBlock.getConversationId(), extractedConvoBlock.getConversationId());
    }

    @Test
    public void someNullFields() throws Exception {
        ConversationBlock originalConvoBlock = new ConversationBlock("convid", 1, Optional.empty(), Optional.empty());

        IRiakObject riakObject = converter.fromDomain(originalConvoBlock, vclock);
        ConversationBlock extractedConvoBlock = converter.toDomain(riakObject);

        assertEquals(originalConvoBlock.getBuyerBlockedSellerAt(), extractedConvoBlock.getBuyerBlockedSellerAt());
        assertEquals(originalConvoBlock.getSellerBlockedBuyerAt(), extractedConvoBlock.getSellerBlockedBuyerAt());
        assertEquals(originalConvoBlock.getVersion(), extractedConvoBlock.getVersion());
        assertEquals(originalConvoBlock.getConversationId(), extractedConvoBlock.getConversationId());
    }
}
