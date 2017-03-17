package com.ecg.replyts.core.runtime.cron;

public final class CronExpressionBuilder {
    private static final int HOUR = 60;
    private static final int DAY = 24 * 60;
    private static final int WEEK = 7 * 24 * 60;

    public static String everyNMinutes(int minutes) {
        if (minutes < 1) {
            throw new IllegalStateException("minutes can't be less than 1");
            //less than an hour
        } else if (minutes <= HOUR) {
            return String.format("0 0/%s * 1/1 * ? *", minutes);
            //every n hours and less than a day
        } else if ((minutes > HOUR && minutes < DAY) && (minutes % HOUR == 0)) {
            return String.format("0 0 0/%s 1/1 * ? *", minutes / HOUR);
            //every n days at 1 am and less than a week
        } else if ((minutes >= DAY && minutes <= WEEK) && (minutes % DAY == 0)) {
            return String.format("0 0 1 1/%s * ? *", minutes / DAY);
        } else {
            throw new IllegalStateException("Invalid number for minutes");
        }
    }

    public static String everyNMinutes(int minutes, int offset) {
        if (minutes < 1) {
            throw new IllegalStateException("minutes cant be less than 1");
            //less than an hour
        } else if (minutes <= HOUR) {
            return String.format("0 %s/%s * 1/1 * ? *", offset, minutes);
            //every n hours and less than a day
        } else if ((minutes > HOUR && minutes < DAY) && (minutes % HOUR == 0)) {
            return String.format("0 %s 0/%s 1/1 * ? *", offset, minutes / HOUR);
            //every n days at 1 am and less than a week
        } else if ((minutes >= DAY && minutes <= WEEK) && (minutes % DAY == 0)) {
            return String.format("0 %s 1 1/%s * ? *", offset, minutes / DAY);
        } else {
            throw new IllegalStateException("Invalid number for minutes");
        }
    }

    public static String everyNSeconds(int seconds) {
        if (seconds < 1 || seconds > 60) {
            throw new IllegalStateException("seconds must be in range [1, 60]");
        }
        return String.format("0/%s * * * * ? *", seconds);
    }
}