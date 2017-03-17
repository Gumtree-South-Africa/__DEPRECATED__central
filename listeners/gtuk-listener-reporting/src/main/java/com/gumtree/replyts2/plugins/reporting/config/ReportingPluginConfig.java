package com.gumtree.replyts2.plugins.reporting.config;

import com.ecg.replyts.core.api.util.CurrentClock;
import com.gumtree.replyts2.plugins.reporting.DataWarehouseEventLogListener;
import com.gumtree.replyts2.plugins.reporting.EventPublisher;
import com.gumtree.replyts2.plugins.reporting.JdbcEventPublisher;
import com.gumtree.replyts2.plugins.reporting.MessageProcessedEvent;
import com.gumtree.replyts2.plugins.reporting.queue.JdbcMessageQueueManager;
import com.gumtree.replyts2.plugins.reporting.queue.MessageQueueEventListener;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;

@Configuration
public class ReportingPluginConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ReportingPluginConfig.class);

    @Value("${gumtree.replyts2.event.log.jdbc.driver:}")
    private String jdbcDriver;

    @Value("${gumtree.replyts2.event.log.jdbc.url:}")
    private String jdbcUrl;

    @Value("${gumtree.replyts2.event.log.jdbc.username:}")
    private String jdbcUsername;

    @Value("${gumtree.replyts2.event.log.jdbc.password:}")
    private String jdbcPassword;

    @Value("${gumtree.replyts2.event.log.jdbc.pool.maxsize:10}")
    private int poolSize;

    @Bean
    public EventPublisher jdbcEventPublisher() {
        try {
            DataSource dataSource = getDataSource();
            return new JdbcEventPublisher(dataSource);
        } catch (Exception ex) {
            LOG.warn("Data warehouse event logging to database is disabled", ex);
            return new DisabledEventPublisher();
        }
    }

    @Bean
    public JdbcMessageQueueManager jdbcMessageQueueManager() {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
        return new JdbcMessageQueueManager(jdbcTemplate);

    }

    @Bean
    public DataSource getDataSource() {
        Assert.hasLength(jdbcDriver, "no jdbc driver configuration found");
        Assert.hasLength(jdbcUrl, "no jdbc url configuration found");
        Assert.hasLength(jdbcUsername, "no jdbc username configuration found");
        Assert.hasLength(jdbcPassword, "no jdbc password configuration found");

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(jdbcDriver);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(jdbcUsername);
        dataSource.setPassword(jdbcPassword);
        dataSource.setMaximumPoolSize(poolSize);

        LOG.info("Data warehouse event logging to database is enabled");
        LOG.info(String.format("Data warehouse event logging using maximum pool size of %d", poolSize));
        return dataSource;
    }

    @Bean
    public DataWarehouseEventLogListener dataWarehouseEventLogPlugin() {
        return new DataWarehouseEventLogListener(jdbcEventPublisher(), new CurrentClock());
    }

    @Bean
    public MessageQueueEventListener messageQueueEventListener() {
        return new MessageQueueEventListener(jdbcMessageQueueManager(), new CurrentClock());
    }

/*    @Bean
    public MessageProcessedTrigger testTrigger() {
        MessageProcessedTrigger trigger = new MessageProcessedTrigger(dataWarehouseEventLogPlugin());
        trigger.run();
        return trigger;
    }*/

    private class DisabledEventPublisher implements EventPublisher {
        @Override
        public void publish(MessageProcessedEvent event) {
            // DO NOTHING
        }
    }
}
