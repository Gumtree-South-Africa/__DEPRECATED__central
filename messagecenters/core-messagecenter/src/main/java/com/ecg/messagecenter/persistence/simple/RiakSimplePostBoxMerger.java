package com.ecg.messagecenter.persistence.simple;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

import static java.util.Arrays.asList;

public class RiakSimplePostBoxMerger {
    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;

    public PostBox mergePrimarySecondary(PostBox<AbstractConversationThread> primary, PostBox secondary) {
        List<PostBox> postBoxesToMerge = asList(primary, secondary);
        Map<String, AbstractConversationThread> latest = mergeAbstractConversationThreads(postBoxesToMerge);

        return new PostBox(
          primary.getEmail(),
          primary.getNewRepliesCounter(), // the primary is more up to date, therefore take its counter
          ImmutableList.copyOf(latest.values()),
          maxAgeDays);
    }

    public PostBox merge(Collection<PostBox> postBoxesToResolve) {
        Map<String, AbstractConversationThread> latest = mergeAbstractConversationThreads(new ArrayList<>(postBoxesToResolve));

        return new PostBox(
          postBoxesToResolve.iterator().next().getEmail(),
          getHighestCounter(postBoxesToResolve), // we'd rather show too many notifications than too few
          ImmutableList.copyOf(latest.values()),
          maxAgeDays);
    }

    private Map<String, AbstractConversationThread> mergeAbstractConversationThreads(List<PostBox> postBoxesToMerge) {
        Map<String, AbstractConversationThread> latest = Maps.newHashMap();
        for (PostBox postBox : postBoxesToMerge) {
            updateLatestThreadVersions(latest, postBox);
        }
        return latest;
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

    private void updateLatestThreadVersions(Map<String, AbstractConversationThread> latestThreadVersions, PostBox<AbstractConversationThread> postBox) {
        for (AbstractConversationThread thread : postBox.getConversationThreads()) {
            putIfNewerVersion(latestThreadVersions, thread);
        }
    }

    private void putIfNewerVersion(Map<String, AbstractConversationThread> latestThreadVersions, AbstractConversationThread thread) {
        String conversationId = thread.getConversationId();
        if (latestThreadVersions.containsKey(conversationId)) {
            AbstractConversationThread previousConversation = latestThreadVersions.get(conversationId);
            if (previousConversation.getModifiedAt().isBefore(thread.getModifiedAt())) {
                latestThreadVersions.put(conversationId, thread);
            }
        } else {
            latestThreadVersions.put(conversationId, thread);
        }
    }
}