package com.ecg.de.ebayk.messagecenter.persistence;

import com.basho.riak.client.cap.Mutation;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * User: maldana
 * Date: 1/8/15
 * Time: 11:14 AM
 *
 * @author maldana@ebay.de
 */
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
