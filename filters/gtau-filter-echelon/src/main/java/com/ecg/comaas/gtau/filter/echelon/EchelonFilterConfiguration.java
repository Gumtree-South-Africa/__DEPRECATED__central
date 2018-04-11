package com.ecg.comaas.gtau.filter.echelon;

public class EchelonFilterConfiguration {
    private String endpointUrl;
    private int endpointTimeout;
    private int score;

    public EchelonFilterConfiguration(String endpointUrl, int endpointTimeout, int score) {
        this.endpointUrl = endpointUrl;
        this.endpointTimeout = endpointTimeout;
        this.score = score;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public int getEndpointTimeout() {
        return endpointTimeout;
    }

    public void setEndpointTimeout(int endpointTimeout) {
        this.endpointTimeout = endpointTimeout;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
