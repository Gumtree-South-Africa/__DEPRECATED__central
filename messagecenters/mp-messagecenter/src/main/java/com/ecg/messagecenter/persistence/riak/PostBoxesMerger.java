package com.ecg.messagecenter.persistence.riak;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.PostBox;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.*;

import static java.util.Arrays.asList;

public class PostBoxesMerger {

    // it differs to merge() as there is a primary (more up to date) data-object
    public PostBox mergePrimarySecondary(PostBox primary, PostBox secondary) {

        List<PostBox> postBoxesToMerge = asList(primary, secondary);
        Map<String, ConversationThread> latest = mergeConversationThreads(postBoxesToMerge);

        return new PostBox(
                primary.getUserId(),
                ImmutableList.copyOf(latest.values()));
    }

    public PostBox merge(Collection<PostBox> postBoxesToResolve) {

        Map<String, ConversationThread> latest = mergeConversationThreads(new ArrayList<>(postBoxesToResolve));

        return new PostBox(
                postBoxesToResolve.iterator().next().getUserId(),
                ImmutableList.copyOf(latest.values()));
    }


    private Map<String, ConversationThread> mergeConversationThreads(List<PostBox> postBoxesToMerge) {
        Map<String, ConversationThread> latest = Maps.newHashMap();
        for (PostBox postBox : postBoxesToMerge) {
            updateLatestThreadVersions(latest, postBox);
        }
        return latest;
    }

    private Optional<Long> getHighestCounter(Collection<PostBox> postBoxesToResolve) {
        long counter = 0;
        for (PostBox postBox : postBoxesToResolve) {
            if (counter < postBox.getNewRepliesCounter()) {
                counter = postBox.getNewRepliesCounter();
            }
        }
        return Optional.of(counter);
    }

    private void updateLatestThreadVersions(Map<String, ConversationThread> latestThreadVersions, PostBox postBox) {
        for (ConversationThread thread : postBox.getConversationThreads()) {
            putIfNewerVersion(latestThreadVersions, thread);
        }
    }

    private void putIfNewerVersion(Map<String, ConversationThread> latestThreadVersions, ConversationThread thread) {
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
