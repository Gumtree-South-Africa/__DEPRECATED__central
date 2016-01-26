
package com.ecg.replyts.core.runtime.workers;

import com.ecg.replyts.app.mailreceiver.MailDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class WorkerPoolConfiguration {

    @Value("${replyts.threadpool.size:2}")
    private int poolSize;

    @Autowired
    private MailDataProvider mailDataProvider;

    @Bean
    public WorkerPoolManager buildWorkerPoolManager() {
        return new WorkerPoolManager(poolSize, mailDataProvider);
    }

}
