package com.ecg.messagecenter.persistence.simple;

import com.basho.riak.client.cap.Mutation;
import com.ecg.messagecenter.persistence.AbstractConversationThread;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Fix for BLN-7065, causing empty message-boxes in certain edge case scenarios.
 */
public class RiakSimplePostBoxMutator implements Mutation<PostBox> {
    private RiakSimplePostBoxMerger postBoxMerger;

    private PostBox postBox;

    private List<String> deletedIds;

    public RiakSimplePostBoxMutator(RiakSimplePostBoxMerger postBoxMerger, PostBox postBox, List<String> deletedConversationIds) {
        this.postBox = postBox;
        this.postBoxMerger = postBoxMerger;
        this.deletedIds = deletedConversationIds;
    }

    @Override
    public PostBox apply(PostBox original) {
        if (original == null) {
            return postBox;
        }

        PostBox merged = postBoxMerger.mergePrimarySecondary(postBox, original);

        removeDeletedConversations(merged);

        return merged;
    }

    private void removeDeletedConversations(PostBox<AbstractConversationThread> merged) {
        List<String> idsToRemove = merged.getConversationThreads()
                .stream()
                .filter((x) -> deletedIds.contains(x.getConversationId()))
                .map((x) -> x.getConversationId())
                .collect(Collectors.toList());

        idsToRemove.stream()
                .forEach((x) -> merged.removeConversation(x));
    }
}
