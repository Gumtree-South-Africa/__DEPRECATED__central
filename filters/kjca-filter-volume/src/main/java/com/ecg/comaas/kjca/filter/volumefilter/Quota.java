package com.ecg.comaas.kjca.filter.volumefilter;

import com.google.common.base.Objects;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Objects.equal;

/**
 * Defines one quota (maximum number of mails allowed within a given time slice).
 * If the quota is exceeded, a score is assigned to the message exceeding the quota.
 * Optionally, the score can be remembered for some time and applied even if the
 * window's passed.
 */
class Quota implements Comparable<Quota>{

    private final int allowance;
    private final int perTimeValue;
    private final TimeUnit perTimeUnit;
    private final int score;
    private final TimeUnit scoreMemoryDurationUnit;
    private final int scoreMemoryDurationValue;

    public Quota(int allowance, int perTimeValue, TimeUnit perTimeUnit, int score, int scoreMemoryDurationValue, TimeUnit scoreMemoryDurationUnit) {
        this.allowance = allowance;
        this.perTimeValue = perTimeValue;
        this.perTimeUnit = perTimeUnit;
        this.score = score;
        this.scoreMemoryDurationValue = scoreMemoryDurationValue;
        this.scoreMemoryDurationUnit = scoreMemoryDurationUnit;
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

    public TimeUnit getScoreMemoryDurationUnit() {
        return scoreMemoryDurationUnit;
    }

    public int getScoreMemoryDurationValue() {
        return scoreMemoryDurationValue;
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof Quota)) {
            return false;
        }
        Quota otherQ=(Quota) other;
        return equal(allowance, otherQ.allowance) &&
                equal(score, otherQ.score) &&
                equal(perTimeUnit.toMillis(perTimeValue), otherQ.perTimeUnit.toMillis(otherQ.perTimeValue)) &&
                equal(
                        scoreMemoryDurationUnit.toMillis(scoreMemoryDurationValue),
                        otherQ.scoreMemoryDurationUnit.toMillis(otherQ.scoreMemoryDurationValue)
                );

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(allowance, perTimeUnit.toMillis(perTimeValue), score, scoreMemoryDurationUnit.toMillis(scoreMemoryDurationValue));
    }

    public int compareTo(Quota quota) {
        return quota.score - score;
    }


    public int getDurationSeconds() {
        return (int) perTimeUnit.toSeconds(perTimeValue);
    }


    public String uihint() {
        return String.format("over max: %s per %s %s", allowance, perTimeValue, perTimeUnit);
    }

    public String describeViolation(long numberOfMailsInTimerange) {
        return String.format("User sent %s mails in %s %s", numberOfMailsInTimerange, perTimeValue, perTimeUnit);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Quota{");
        sb.append("allowance=").append(allowance);
        sb.append(", perTimeValue=").append(perTimeValue);
        sb.append(", perTimeUnit=").append(perTimeUnit);
        sb.append(", score=").append(score);
        sb.append(", scoreMemoryDurationUnit=").append(scoreMemoryDurationUnit);
        sb.append(", scoreMemoryDurationValue=").append(scoreMemoryDurationValue);
        sb.append('}');
        return sb.toString();
    }
}
