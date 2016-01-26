package com.ecg.replyts.core.runtime;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.runtime.cron.CronJobService;
import com.ecg.replyts.core.runtime.cron.DistributedExecutionStatusMonitor;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;


/**
 * initializes all beans that manage plugin capability from country specific code.
 * <p/>
 * <b>Implementation note: </b> Sorting out the generics in this class turns out to be pretty tough. Therefore I decided
 * to "ignore" them and suppress all warnings. There is no value in keeping the informations here correct as spring will
 * not take care of correct generic type info anyhow. Thus keeping those generics correct in here would make the
 * internal code of {@link com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory} much more tricky.
 *
 * @author mhuttar
 */
@Configuration
class ModuleSystemConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleSystemConfiguration.class);


    @Autowired(required = true)
    private List<CronJobExecutor> executors = Collections.emptyList();

    @Autowired(required = true)
    private HazelcastInstance hazelcast;

    @Value("${node.passive:false}")
    private boolean disableCronjobs;



    @Bean(name = "cronJobService")
    public CronJobService buildCronJobService(DistributedExecutionStatusMonitor distributedExecutionStatusMonitor) {
        return new CronJobService(executors, distributedExecutionStatusMonitor, hazelcast, disableCronjobs);
    }

    @Bean
    public DistributedExecutionStatusMonitor executionStatusMonitor(HazelcastInstance hazelcast) {
        return new DistributedExecutionStatusMonitor(hazelcast);
    }

    @PreDestroy
    void shutdown() {
        LOG.info("Stopping Hazelcast Cluster");
        Hazelcast.shutdownAll();
    }
}
