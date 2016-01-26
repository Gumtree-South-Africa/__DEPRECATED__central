package com.ecg.replyts.core.api.cron;

import com.google.common.base.Objects;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.ecg.replyts.core.api.util.Pairwise.pairsAreEqual;

public final class CronExecution implements Serializable {
    private String jobName;
    private DateTime startDate;
    private String executedOnHost;

    CronExecution(String jobName, DateTime startDate, String executedOnHost) {
        this.jobName = jobName;
        this.startDate = startDate;
        this.executedOnHost = executedOnHost;
    }

    public String getJobName() {
        return jobName;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public String getExecutedOnHost() {
        return executedOnHost;
    }

    public static CronExecution forJob(Class<? extends CronJobExecutor> job) {
        try {
            return new CronExecution(job.getName(), DateTime.now(), InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CronExecution that = (CronExecution) o;

        return pairsAreEqual(this.jobName, that.jobName, this.startDate, that.startDate, this.executedOnHost, that.executedOnHost);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(jobName, startDate, executedOnHost);
    }
}
