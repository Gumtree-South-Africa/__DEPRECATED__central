package com.ecg.comaas.mde.postprocessor.demandreporting.usertracking;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

class AdRef {
    final Long id;
    final Long version;
    final Long sellerId;

    private AdRef(Long id, @Nullable Long version, @Nullable Long sellerId) {
        this.id = requireNonNull(id);
        this.version = version;
        this.sellerId = sellerId;
    }

    static AdRef of(@Nullable Long id, @Nullable Long version, @Nullable Long sellerId) {
        if (id == null) return UNKNOWN;
        return new AdRef(id, version, sellerId);
    }

    static AdRef UNKNOWN = new AdRef(-1L, null, null);
}
