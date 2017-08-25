package com.ecg.messagecenter.persistence;

import com.basho.riak.client.cap.ConflictResolver;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 16:26
 *
 * @author maldana@ebay.de
 */
class PostBoxConflictResolver implements ConflictResolver<PostBox> {

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxConflictResolver.class);

    private static final Counter POSTBOX_MERGE_COUNTER = TimingReports.newCounter("riak-postbox-merges");
    private static final Histogram POSTBOX_SIBLING_COUNT_HISTOGRAM = TimingReports.newHistogram("riak-postbox-sibling-counts");
    private int maxAgeDays;

    public PostBoxConflictResolver(int maxAgeDays) {
        this.maxAgeDays = maxAgeDays;
    }


    @Override public PostBox<ConversationThread> resolve(Collection<PostBox> postBoxesToResolve) {
        if (postBoxesToResolve.isEmpty()) {
            return null;
        }

        // by definition size()==1 is NO conflict (sibling == 1 is reflexive)
        if (postBoxesToResolve.size() == 1) {
            return postBoxesToResolve.iterator().next();
        }

        POSTBOX_MERGE_COUNTER.inc();
        POSTBOX_SIBLING_COUNT_HISTOGRAM.update(postBoxesToResolve.size());

        if (postBoxesToResolve.size() > 5) {
            LOG.info("High number '{}' of siblings found for postbox #{}",
                            postBoxesToResolve.size(),
                            postBoxesToResolve.iterator().next().getEmail());
        }


        Map<String, ConversationThread> latest = Maps.newHashMap();
        for (PostBox postBox : postBoxesToResolve) {
            updateLatestThreadVersions(latest, postBox);
        }


        return new PostBox<>(postBoxesToResolve.iterator().next().getEmail(), getHighestCounter(postBoxesToResolve), ImmutableList.copyOf(latest.values()), maxAgeDays);

    }

    private Optional<Long> getHighestCounter(Collection<PostBox> postBoxesToResolve) {
        long counter = 0;
        for (PostBox postBox : postBoxesToResolve) {
            if (counter < postBox.getNewRepliesCounter().getValue()) {
                counter = postBox.getNewRepliesCounter().getValue();
            }
        }
        return Optional.of(counter);
    }

    private void updateLatestThreadVersions(Map<String, ConversationThread> latestThreadVersions,
                    PostBox<ConversationThread> postBox) {
        for (ConversationThread thread : postBox.getConversationThreads()) {
            putIfNewerVersion(latestThreadVersions, thread);
        }
    }

    private void putIfNewerVersion(Map<String, ConversationThread> latestThreadVersions,
                    ConversationThread thread) {
        String conversationId = thread.getConversationId();
        if (latestThreadVersions.containsKey(conversationId)) {
            ConversationThread previousConversation = latestThreadVersions.get(conversationId);
            if (previousConversation.getModifiedAt().isBefore(thread.getModifiedAt())) {
                latestThreadVersions.put(conversationId, thread);
            }
        } else {
            latestThreadVersions.put(conversationId, thread);
        }
    }


}
