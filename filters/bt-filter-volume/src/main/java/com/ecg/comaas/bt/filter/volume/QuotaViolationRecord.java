package com.ecg.comaas.bt.filter.volume;

import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * Represents a remembered violation for a single email address / sender.
 * Description is shown in the UI, so should not contain any user-entered
 * data.
 */
public class QuotaViolationRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer score;
    private String description;

    public QuotaViolationRecord(Integer score, String description) {
        this.description = description;
        this.score = score;
    }

    public Integer getScore() {
        return score;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuotaViolationRecord that = (QuotaViolationRecord) o;
        return Objects.equal(description, that.description) &&
                Objects.equal(score, that.score);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(description, score);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("QuotaViolationRecord{");
        sb.append("description='").append(description).append('\'');
        sb.append(", score=").append(score);
        sb.append('}');
        return sb.toString();
    }
}