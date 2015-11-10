package nl.marktplaats.filter.volume;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Configuration Object for a Volume Filter. This configuration consists of several rules, that may define quotas and scores to assign to such mails individually.
 * If a mail violates more than one rule, the maximum score will be assigned to it.
 *
 * @author huttar
 */
public class VolumeFilterConfiguration {

    private List<VolumeRule> config;

    /**
     * Represents a Volume Configuration Rule. Each rule defines a {@link #score}, that
     * is added to a message, when the sender has sent more than {@link #maxCount} mails in the last {@link #timeSpan} {@link #timeUnit}. <p>
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
     *
     * @author huttar
     */
    public static class VolumeRule {

        private long timeSpan;
        private TimeUnit timeUnit;
        private long maxCount;
        private int score;


        /**
         * Initializes a Volume Rule with custom values
         *
         * @param timeSpan
         * @param timeUnit
         * @param maxCount
         * @param score
         */
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
         * @param timeSpan timespan of this particular rule.
         */
        public void setTimeSpan(long timeSpan) {
            this.timeSpan = timeSpan;
        }

        /**
         * @param maxCount max number of messages that is allowed in this time frame
         */
        public void setMaxCount(long maxCount) {
            this.maxCount = maxCount;
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
         * @param timeUnit the unit of time measurement to take into account
         */
        public void setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
        }


        /**
         * @return score mails will get added if they exceed the allowed limit
         */
        public int getScore() {
            return score;
        }

        /**
         * @param score score mails will get added if they exceed the allowed limit
         */
        public void setScore(int score) {
            this.score = score;
        }

        @Override
        public String toString() {
            return String.format("[if >= %s mails per %s %s. Score+=%s]", maxCount, timeSpan, timeUnit, score);
        }
    }

    /**
     * Sets all Volume Configuration Rules
     *
     * @param config
     */
    public void setConfig(List<VolumeRule> config) {
        this.config = config;
    }

    /**
     * @return a list of Volum Configuration Rules
     */
    public List<VolumeRule> getConfig() {
        return config;
    }
}
