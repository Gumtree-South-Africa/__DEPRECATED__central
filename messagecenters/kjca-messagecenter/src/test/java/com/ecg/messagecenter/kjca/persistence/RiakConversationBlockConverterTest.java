package com.ecg.messagecenter.kjca.persistence;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.cap.BasicVClock;
import com.basho.riak.client.cap.VClock;
import com.ecg.messagecenter.kjca.persistence.block.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RiakConversationBlockConverterTest.TestContext.class })
@TestPropertySource(properties = {
  "persistence.strategy = riak"
})
public class RiakConversationBlockConverterTest {
    @Autowired
    private RiakConversationBlockConverter converter;

    private VClock vclock = new BasicVClock(new byte[] {});

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

    @Configuration
    @Import({ JsonToConversationBlockConverter.class, ConversationBlockToJsonConverter.class })
    static class TestContext {
        @Bean
        public RiakConversationBlockConverter converter() {
            return new RiakConversationBlockConverter();
        }
    }
}
