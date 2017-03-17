package com.ecg.replyts.core.runtime.cron;

import org.junit.Assert;
import org.junit.Test;
import static com.ecg.replyts.core.runtime.cron.CronExpressionBuilder.everyNMinutes;

public class CronExpressionBuilderTest {
    @Test
    public void generatesValidCronExpressionWhenPassingLessThanAnHourFrequency() throws Exception {
        String cronExpression = everyNMinutes(45);

        Assert.assertEquals("0 0/45 * 1/1 * ? *",cronExpression);
    }

    @Test
    public void generatesValidCronExpressionWhenPassingMoreThanAnHourAndLessThanADayFrequency() throws Exception {
        String cronExpression = everyNMinutes(1380);

        Assert.assertEquals("0 0 0/23 1/1 * ? *",cronExpression);
    }

    @Test
    public void generatesValidCronExpressionWhenPassingMoreThanADayAndLessThanAWeekFrequency() throws Exception {
        String cronExpression = everyNMinutes(10080);

        Assert.assertEquals("0 0 1 1/7 * ? *",cronExpression);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionWhenNotValidMinutesNumber() throws Exception {
        everyNMinutes(4321);
    }

    @Test
    public void generatesValidCronExpressionWhenPassingLessThanAnHourFrequencyWithOffset() throws Exception {
        String cronExpression = everyNMinutes(45, 15);

        Assert.assertEquals("0 15/45 * 1/1 * ? *",cronExpression);
    }

    @Test
    public void generatesValidCronExpressionWhenPassingMoreThanAnHourAndLessThanADayFrequencyWithOffset() throws Exception {
        String cronExpression = everyNMinutes(1380, 15);

        Assert.assertEquals("0 15 0/23 1/1 * ? *",cronExpression);
    }

    @Test
    public void generatesValidCronExpressionWhenPassingMoreThanADayAndLessThanAWeekFrequencyWithOffset() throws Exception {
        String cronExpression = everyNMinutes(10080, 15);

        Assert.assertEquals("0 15 1 1/7 * ? *",cronExpression);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionWhenNotValidMinutesNumberWithOffset() throws Exception {
        everyNMinutes(4321, 15);
    }
}
