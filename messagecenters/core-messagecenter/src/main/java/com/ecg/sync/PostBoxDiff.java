package com.ecg.sync;

import com.ecg.messagecenter.persistence.simple.PostBox;

public class PostBoxDiff {

    private final PostBox postBox;
    private final PostBoxResponse postBoxResponse;

    public PostBoxDiff(PostBox postBox, PostBoxResponse postBoxResponse) {
        this.postBox = postBox;
        this.postBoxResponse = postBoxResponse;
    }

    public PostBox getPostBox() {
        return postBox;
    }

    public PostBoxResponse getPostBoxResponse() {
        return postBoxResponse;
    }
}
