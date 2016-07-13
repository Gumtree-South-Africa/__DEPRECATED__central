package com.ecg.de.mobile.replyts.demand.usertracking;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

class AdRef {
    final Long id;
    final Long version;

    private AdRef(Long id, @Nullable Long version) {
        this.id = requireNonNull(id);
        this.version = version;
    }

    static AdRef of(@Nullable Long id, @Nullable Long version) {
        if (id == null) return UNKNOWN;
        return new AdRef(id, version);
    }

    static AdRef UNKNOWN = new AdRef(-1L, null);
}
