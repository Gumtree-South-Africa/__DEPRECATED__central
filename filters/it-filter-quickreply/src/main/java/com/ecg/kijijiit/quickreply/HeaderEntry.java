package com.ecg.kijijiit.quickreply;

/**
 * Created by fmaffioletti on 28/07/14.
 */
public class HeaderEntry {

    private final String header;
    private final Integer score;

    public HeaderEntry(String header, Integer score) {
        this.header = header;
        this.score = score;
    }

    public String getHeader() {
        return header;
    }

    public Integer getScore() {
        return score;
    }

    @Override public String toString() {
        return "HeaderEntry{" +
                        "header='" + header + '\'' +
                        ", score=" + score +
                        '}';
    }
}
