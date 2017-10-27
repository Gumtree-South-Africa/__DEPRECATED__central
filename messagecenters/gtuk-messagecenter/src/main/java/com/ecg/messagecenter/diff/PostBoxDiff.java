package com.ecg.messagecenter.diff;

import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;

class PostBoxDiff {

    final PostBox postBox;

    final PostBoxResponse postBoxResponse;

    private PostBoxDiff(PostBox postBox, PostBoxResponse postBoxResponse) {
        this.postBox = postBox;
        this.postBoxResponse = postBoxResponse;
    }

    static PostBoxDiff of(PostBox postBox, PostBoxResponse postBoxResponse) {
        return new PostBoxDiff(postBox, postBoxResponse);
    }
}
