package com.ecg.messagecenter.persistence;

import com.basho.riak.client.cap.ConflictResolver;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.Optional;

/**
 * Riak resolver for multiple siblings in user conversation block states.
 *
 * The resolution is inherently arbitrary. It will error on the side of the seller,
 * and will prefer to block. So, for example, if the seller very quickly blocked and
 * unblocked the buyer a few times and it caused conflicts, this implementation will
 * pick the configuration that has the buyer blocked. The seller side is preferred
 * because the assumption is that it's more likely that the seller will be getting
 * nasty messages. After all, they're opening themselves up for abuse just by posting
 * an ad.
 *
 * If the block state is the same, it'll pick the config with the latest block date.
 * Again, looking at seller->buyer block first, and then buyer->seller.
 */
public class ConversationBlockConflictResolver implements ConflictResolver<ConversationBlock> {
    @Override
    public ConversationBlock resolve(Collection<ConversationBlock> siblings) {
        if (siblings.isEmpty()) {
            return null;
        }

        if (siblings.size() == 1) {
            return siblings.iterator().next();
        }

        return siblings.stream()
                .reduce((first, second) ->
                        latestBlock(first, first.getSellerBlockedBuyerAt(), second, second.getSellerBlockedBuyerAt())
                                .orElse(latestBlock(first, first.getBuyerBlockedSellerAt(), second, second.getBuyerBlockedSellerAt())
                                        .orElse(second)))
                .orElse(siblings.iterator().next());
    }

    private Optional<ConversationBlock> latestBlock(
            ConversationBlock first,
            Optional<DateTime> firstBlock,
            ConversationBlock second,
            Optional<DateTime> secondBlock
    ) {
        if (!secondBlock.isPresent()) {
            return firstBlock.isPresent() ? Optional.of(first) : Optional.empty();
        }

        if (!firstBlock.isPresent()) {
            return Optional.of(second);
        }

        if (firstBlock.get().equals(secondBlock.get())) { // no conflict -> caller has to decide
            return Optional.empty();
        }

        return firstBlock.get().isAfter(secondBlock.get()) ? Optional.of(first) : Optional.of(second);
    }
}
