package com.ecg.replyts.core.runtime.cron;

/**
 * utility methods to create some cron job expressions.
 *
 * @author mhuttar
 */
public final class CronExpressionBuilder {
    private CronExpressionBuilder() {

    }


    public static String everyNMinutes(int minutes) {
        if (minutes < 1 || minutes > 60) {
            throw new IllegalStateException("minutes must be in range [1, 60]");
        }
        return String.format("0 0/%s * 1/1 * ? *", minutes);
    }

    public static String everyNMinutes(int minutes, int offset) {
        if (minutes < 1 || minutes > 60) {
            throw new IllegalStateException("minutes must be in range [1, 60]");
        }
        return String.format("0 %s/%s * 1/1 * ? *", offset, minutes);
    }

    public static String never() {
        return "0 0 0 1 1 ? 2099";
    }


    public static String everyNSeconds(int seconds) {
        if (seconds < 1 || seconds > 60) {
            throw new IllegalStateException("seconds must be in range [1, 60]");
        }
        return String.format("0/%s * * * * ? *", seconds);

    }

}