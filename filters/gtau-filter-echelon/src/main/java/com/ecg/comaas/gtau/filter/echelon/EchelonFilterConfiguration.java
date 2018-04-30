package com.ecg.comaas.gtau.filter.echelon;

import com.google.common.base.MoreObjects;

public final class EchelonFilterConfiguration {

    private final String endpointUrl;
    private final int endpointTimeout;
    private final int score;

    public EchelonFilterConfiguration(String endpointUrl, int endpointTimeout, int score) {
        this.endpointUrl = endpointUrl;
        this.endpointTimeout = endpointTimeout;
        this.score = score;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public int getEndpointTimeout() {
        return endpointTimeout;
    }

    public int getScore() {
        return score;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("endpointUrl", endpointUrl)
                .add("endpointTimeout", endpointTimeout)
                .add("score", score)
                .toString();
    }
}
