package com.ecg.comaas.mde.postprocessor.demandreporting.usertracking;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

class Vi {
    final String cid;
    final String sub;
    final Boolean doNotTrack;

    private Vi(String cid, @Nullable String sub, @Nullable Boolean doNotTrack) {
        this.cid = requireNonNull(cid);
        this.sub = sub;
        this.doNotTrack = doNotTrack;
    }

    static Vi of(@Nullable String cid, @Nullable String sub, @Nullable Boolean doNotTrack) {
        if (cid == null) return UNKNOWN;
        return new Vi(cid, sub, doNotTrack);
    }

    static Vi UNKNOWN = new Vi("unknown", null, null);
}
