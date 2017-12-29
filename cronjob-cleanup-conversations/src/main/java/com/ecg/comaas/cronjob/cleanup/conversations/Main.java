package com.ecg.comaas.cronjob.cleanup.conversations;

import com.ecg.replyts.app.cronjobs.cleanup.CassandraCleanupConversationCronJob;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerShutdownHook();

        context.register(SpringConfiguration.class);
        context.refresh();
        CassandraCleanupConversationCronJob cronjob = context.getBean(CassandraCleanupConversationCronJob.class);

        cronjob.createThreadPoolExecutor();
        cronjob.execute();

        context.close();
    }
}
