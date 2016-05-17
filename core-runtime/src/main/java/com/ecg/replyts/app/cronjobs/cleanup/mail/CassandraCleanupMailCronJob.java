package com.ecg.replyts.app.cronjobs.cleanup.mail;

import com.ecg.replyts.app.cronjobs.cleanup.CleanupDateCalculator;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.model.mail.MailCreationDate;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.mail.CassandraMailRepository;
import com.google.common.collect.Iterators;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.joda.time.DateTime.now;

public class CassandraCleanupMailCronJob implements CronJobExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraCleanupMailCronJob.class);
    protected static final String CLEANUP_MAIL_JOB_NAME = "cleanupMailJob";

    @Autowired
    private CassandraMailRepository mailRepository;
    @Autowired
    private CronJobClockRepository cronJobClockRepository;

    private final ThreadPoolExecutor threadPoolExecutor;
    private final int maxMailAgeDays;
    private final int batchSize;
    private final String cronJobExpression;

    public CassandraCleanupMailCronJob(int workQueueSize,
                                       int threadCount,
                                       int maxMailAgeDays,
                                       int batchSize,
                                       String cronJobExpression) {

        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        this.threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);

        this.maxMailAgeDays = maxMailAgeDays;
        this.batchSize = batchSize;
        this.cronJobExpression = cronJobExpression;
    }

    @Override
    public void execute() throws Exception {
        final DateTime cleanupDate = createCleanupDateCalculator().getCleanupDate(maxMailAgeDays, CLEANUP_MAIL_JOB_NAME);
        if (cleanupDate == null) {
            return;
        }

        LOG.info("Cleanup: Deleting mails for the date '{}'", cleanupDate);

        Stream<MailCreationDate> mailCreationDatesToDelete = mailRepository.streamMailCreationDatesByDay(cleanupDate.getYear(),
                cleanupDate.getMonthOfYear(), cleanupDate.getDayOfMonth());

        List<Future<?>> cleanUpTasks = new ArrayList<>();

        Iterators.partition(mailCreationDatesToDelete.iterator(), batchSize).forEachRemaining(idxs -> {
            cleanUpTasks.add(threadPoolExecutor.submit(() -> {
                LOG.info("ThreadPoolExecutor completed task count: " + threadPoolExecutor.getCompletedTaskCount());
                LOG.info("ThreadPoolExecutor active threads: " + threadPoolExecutor.getActiveCount());
                LOG.info("ThreadPoolExecutor current pool size: " + threadPoolExecutor.getPoolSize());
                LOG.info("ThreadPoolExecutor queue size: " + threadPoolExecutor.getQueue().size());
                LOG.info("Cleanup: Deleting data related to {} mail creation dates", idxs.size());
                idxs.forEach(mailCreationDate -> {
                    try {
                        mailRepository.deleteMail(mailCreationDate.getMailId(), mailCreationDate.getCreationDateTime());
                    } catch (RuntimeException ex) {
                        LOG.error("Cleanup: Could not delete Mail: " + mailCreationDate.getMailId(), ex);
                    }
                });
            }));
        });

        cleanUpTasks.stream().filter(task -> !task.isDone()).forEach(task -> {
            try {
                task.get();
            } catch (CancellationException | ExecutionException ignore) {
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        cronJobClockRepository.set(CLEANUP_MAIL_JOB_NAME, now(), cleanupDate);

        LOG.info("Cleanup: Finished deleting mails.");
    }

    @Override
    public String getPreferredCronExpression() {
        return cronJobExpression;
    }

    protected CleanupDateCalculator createCleanupDateCalculator() {
        return new CleanupDateCalculator(cronJobClockRepository);
    }
}
