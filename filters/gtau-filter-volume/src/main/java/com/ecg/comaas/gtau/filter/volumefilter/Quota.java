package com.ecg.comaas.gtau.filter.volumefilter;

import com.google.common.base.Objects;

import java.util.concurrent.TimeUnit;

/**
 * defines one quota (maximum number of mails allowed within a given time slice). if the quota is exceeded, a score is assigned to the message exceeding the quota.
 * @author mhuttar
 */
class Quota implements Comparable<Quota>{

    private final int allowance;

    private final int perTimeValue;

    private final TimeUnit perTimeUnit;

    private final int score;


    public Quota(int allowance, int perTimeValue, TimeUnit perTimeUnit, int score) {
        this.allowance = allowance;
        this.perTimeValue = perTimeValue;
        this.perTimeUnit = perTimeUnit;
        this.score = score;
    }

    public int getAllowance() {
        return allowance;
    }

    public int getPerTimeValue() {
        return perTimeValue;
    }

    public TimeUnit getPerTimeUnit() {
        return perTimeUnit;
    }

    public int getScore() {
        return score;
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof Quota)) {
            return false;
        }
        Quota otherQ=(Quota) other;
        return Objects.equal(allowance, otherQ.allowance) &&
                Objects.equal(score, otherQ.score) &&
                Objects.equal(perTimeUnit.toMillis(perTimeValue), otherQ.perTimeUnit.toMillis(otherQ.perTimeValue));

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(allowance, perTimeUnit.toMillis(perTimeValue), score);
    }

    @Override
    public int compareTo(Quota quota) {
        return quota.score - score;
    }


    public int getDurationMinutes() {
        return (int)perTimeUnit.toMinutes(perTimeValue);
    }


    public String uihint() {
        return String.format("max %s/%s %s", allowance, perTimeValue, perTimeUnit);
    }

    public String describeViolation(long numberOfMailsInTimerange) {
        return String.format("User sent %s mails in %s %s", numberOfMailsInTimerange, perTimeValue, perTimeUnit);
    }

}
