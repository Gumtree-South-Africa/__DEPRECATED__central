package com.ecg.messagecenter.persistence.riak;

import com.basho.riak.client.cap.Mutation;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.PostBox;

import java.util.ArrayList;
import java.util.List;

public class RiakPostBoxMutator implements Mutation<PostBox> {

    private final PostBox postBox;
    private final PostBoxesMerger postBoxesMerger;
    private final List<String> deletedIds;

    public RiakPostBoxMutator(PostBox postBox, List<String> deletedConversationIds) {
        this.postBox = postBox;
        this.postBoxesMerger = new PostBoxesMerger();
        this.deletedIds = deletedConversationIds;
    }

    @Override
    public PostBox apply(PostBox original) {
        if (original == null) {
            return postBox;
        }

        PostBox merged = postBoxesMerger.mergePrimarySecondary(postBox, original);

        removeDeletedConversations(merged);

        return merged;
    }

    private void removeDeletedConversations(PostBox merged) {
        List<String> idsToRemove = new ArrayList<>();

        for (ConversationThread conversationThread : merged.getConversationThreads()) {
            if (deletedIds.contains(conversationThread.getConversationId())) {
                idsToRemove.add(conversationThread.getConversationId());
            }
        }

        for (String idToRemove : idsToRemove) {
            merged.removeConversation(idToRemove);
        }
    }

}