package com.ecg.comaas.r2cmigration.difftool;

import com.datastax.driver.core.Session;
import com.ecg.comaas.r2cmigration.difftool.repo.CassPostboxRepo;
import com.ecg.comaas.r2cmigration.difftool.util.InstrumentedCallerRunsPolicy;
import com.ecg.de.kleinanzeigen.replyts.graphite.GraphiteExporter;
import com.ecg.messagecenter.persistence.JsonToPostBoxConverter;
import com.ecg.messagecenter.persistence.PostBoxToJsonConverter;
import com.ecg.messagecenter.persistence.simple.*;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.core.runtime.persistence.RiakHostConfig;
import com.ecg.replyts.core.runtime.persistence.strategy.CassandraPersistenceConfiguration;
import com.ecg.replyts.core.runtime.persistence.strategy.RiakPersistenceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@ComponentScan("com.ecg.comaas.r2cmigration.difftool")
@PropertySource("file:${confDir}/difftool.properties")
@Import({
        RiakHostConfig.class,
        RiakPersistenceConfiguration.RiakClientConfiguration.class,
        CassandraPersistenceConfiguration.CassandraClientConfiguration.class,
        JacksonAwareObjectMapperConfigurer.class,
        JsonToPostBoxConverter.class,     // This has to be specific for each tenant!
        PostBoxToJsonConverter.class      // This has to be specific for each tenant!
})
public class DiffToolConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(DiffToolConfiguration.class);

    public static final String RIAK_CONVERSATION_BUCKET_NAME = "conversation";

    public static final String RIAK_POSTBOX_BUCKET_NAME = "postbox";

    public static final String RIAK_SECONDARY_INDEX_MODIFIED_AT = "modifiedAt";

    public static final String DATETIME_STRING = "dd-MM-yyyy'T'HH:mm";

    @Value("${graphite.enabled:true}")
    boolean isEnabled;
    @Value("${graphite.endpoint.hostname:graph001}")
    String hostname;
    @Value("${graphite.endpoint.port:2003}")
    int port;
    @Value("${graphite.timeperiod.sec:10}")
    int timeout;
    @Value("${graphite.prefix:difftool}")
    String prefix;

    @Value("${replyts.maxConversationAgeDays:180}")
    int maxEntityAge;

    @Value("${difftool.batch.size:1000}")
    int idBatchSize;
    @Value("${difftool.threadcount:6}")
    int threadCount;
    @Value("${difftool.queue.size:100}")
    int workQueueSize;

    @Bean
    GraphiteExporter graphiteExporter() {
        try {
            return new GraphiteExporter(isEnabled, hostname, port, timeout, prefix);
        } catch (UnknownHostException he) {
            LOG.error("Failed to connect to Graphite with : " + he.getLocalizedMessage(), he);
        }
        return null;
    }

    @Bean
    public R2CConversationDiffTool r2CConversationDiffTool() {
        return new R2CConversationDiffTool(idBatchSize, maxEntityAge);
    }

    @Bean
    public R2CPostboxDiffTool r2CPostboxDiffTool() {
        return new R2CPostboxDiffTool(idBatchSize, maxEntityAge);
    }

    @Bean
    public RiakSimplePostBoxConflictResolver postBoxConflictResolver() {
        return new RiakSimplePostBoxConflictResolver();
    }

    @Bean
    public RiakSimplePostBoxConverter postBoxConverter() {
        return new RiakSimplePostBoxConverter();
    }

    @Bean
    public RiakSimplePostBoxMerger postBoxMerger() {
        return new RiakSimplePostBoxMerger();
    }

    // Uses the same consistency as Conversation repo
    @Bean
    public CassPostboxRepo postBoxRepository(Session cassandraSession) {
        return new CassPostboxRepo(cassandraSession);
    }

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(workQueueSize),
                new InstrumentedCallerRunsPolicy("difftool-conversation", ""));
    }

}
