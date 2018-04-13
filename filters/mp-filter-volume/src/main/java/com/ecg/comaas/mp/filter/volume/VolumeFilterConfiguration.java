package com.ecg.comaas.mp.filter.volume;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Configuration Object for a Volume Filter.
 *
 * This configuration consists of several rules, each defines a quota and a mail score.
 * If a mail violates more than one rule, the maximum score is assigned.
 */
public class VolumeFilterConfiguration {

    private final List<VolumeRule> config;

    public VolumeFilterConfiguration(List<VolumeRule> config) {
        this.config = config;
    }

    /**
     * Represents a Volume Configuration Rule. Each rule defines a {@link #score}, that
     * is assigned to a message when the sender has sent more than {@link #maxCount} mails
     * in the last {@link #timeSpan} {@link #timeUnit}. <p>
     *
     * Example:
     * <p/>
     * Let
     * <pre>
     * score:=50
     * maxCount:=20
     * timeSpan:=10
     * timeUnit=MINUTES
     * </pre>
     * then,
     * <em>All mails by a user, who sent more than 20 mails in 10 Minutes, will get a score of 50 assigned.</em>
     */
    public static class VolumeRule {
        private final long timeSpan;
        private final TimeUnit timeUnit;
        private final long maxCount;
        private final int score;

        public VolumeRule(long timeSpan, TimeUnit timeUnit, long maxCount, int score) {
            this.timeSpan = timeSpan;
            this.timeUnit = timeUnit;
            this.maxCount = maxCount;
            this.score = score;
        }

        /**
         * @return the time span of this particular rule
         */
        public long getTimeSpan() {
            return timeSpan;
        }

        /**
         * @return max number of messages that is allowed in this time frame
         */
        public long getMaxCount() {
            return maxCount;
        }

        /**
         * @return the unit of time measurement to take into account
         */
        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        /**
         * @return score mails will get added if they exceed the allowed limit
         */
        public int getScore() {
            return score;
        }

        @Override
        public String toString() {
            return String.format("[if >= %s mails per %s %s then score+=%s]", maxCount, timeSpan, timeUnit, score);
        }
    }

    /**
     * @return a list of Volume Configuration Rules
     */
    public List<VolumeRule> getConfig() {
        return config;
    }
}
