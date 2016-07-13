package com.ecg.de.mobile.replyts.demand.usertracking;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

class Vi {
    final String cid;
    final String sub;

    private Vi(String cid, @Nullable String sub) {
        this.cid = requireNonNull(cid);
        this.sub = sub;
    }

    static Vi of(@Nullable String cid, @Nullable String sub) {
        if (cid == null) return UNKNOWN;
        return new Vi(cid, sub);
    }

    static Vi UNKNOWN = new Vi("unknown", null);
}
