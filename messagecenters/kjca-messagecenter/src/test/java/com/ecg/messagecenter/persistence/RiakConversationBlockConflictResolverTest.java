package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.messagecenter.persistence.block.RiakConversationBlockConflictResolver;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RiakConversationBlockConflictResolverTest {
    public static final String CONV_ID = "convId";
    private RiakConversationBlockConflictResolver resolver;

    @Before
    public void setUp() throws Exception {
        resolver = new RiakConversationBlockConflictResolver();
    }

    @Test
    public void zeroSiblings_nothingToResolve() throws Exception {
        assertNull(resolver.resolve(ImmutableList.of()));
    }

    @Test
    public void noConflicts_returnSoleSibling() throws Exception {
        Optional<DateTime> now = Optional.of(DateTime.now(DateTimeZone.UTC));

        ConversationBlock originalConversationBlock = new ConversationBlock(CONV_ID, 1, now, Optional.empty());
        ConversationBlock resolvedConversationBlock = resolver.resolve(ImmutableList.of(originalConversationBlock));
        assertEquals(originalConversationBlock, resolvedConversationBlock);
    }

    @Test
    public void favourLatestSellerBlocker() throws Exception {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        DateTime earlier = now.minusSeconds(1);

        ConversationBlock buyerNotBlocked = new ConversationBlock(CONV_ID, 1, Optional.empty(), Optional.empty());
        ConversationBlock buyerBlockedEarlier = new ConversationBlock(CONV_ID, 1, Optional.empty(), Optional.of(earlier));
        ConversationBlock buyerBlockedMostRecently = new ConversationBlock(CONV_ID, 1, Optional.empty(), Optional.of(now));

        // Check different order
        ConversationBlock resolvedBlock = resolver.resolve(ImmutableList.of(buyerBlockedEarlier, buyerBlockedMostRecently, buyerNotBlocked));
        assertEquals(buyerBlockedMostRecently, resolvedBlock);

        resolvedBlock = resolver.resolve(ImmutableList.of(buyerBlockedMostRecently, buyerBlockedEarlier, buyerNotBlocked));
        assertEquals(buyerBlockedMostRecently, resolvedBlock);

        resolvedBlock = resolver.resolve(ImmutableList.of(buyerNotBlocked, buyerBlockedMostRecently, buyerBlockedEarlier));
        assertEquals(buyerBlockedMostRecently, resolvedBlock);
    }

    @Test
    public void favourLatestBuyerBlockerIfSellerIsSame() throws Exception {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        DateTime earlier = now.minusSeconds(1);

        // Seller blocking buyer at the same time. Vary buyer blocking seller.
        ConversationBlock sellerNotBlocked = new ConversationBlock(CONV_ID, 1, Optional.empty(), Optional.of(earlier));
        ConversationBlock sellerBlockedEarlier = new ConversationBlock(CONV_ID, 1, Optional.of(earlier), Optional.of(earlier));
        ConversationBlock sellerBlockedMostRecently = new ConversationBlock(CONV_ID, 1, Optional.of(now), Optional.of(earlier));

        // Check different order
        ConversationBlock resolvedBlock = resolver.resolve(ImmutableList.of(sellerBlockedEarlier, sellerBlockedMostRecently, sellerNotBlocked));
        assertEquals(sellerBlockedMostRecently, resolvedBlock);
    }
}
