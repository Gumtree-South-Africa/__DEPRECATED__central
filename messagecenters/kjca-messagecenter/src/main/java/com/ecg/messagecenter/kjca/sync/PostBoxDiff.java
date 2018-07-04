package com.ecg.messagecenter.kjca.sync;

import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxResponse;

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
