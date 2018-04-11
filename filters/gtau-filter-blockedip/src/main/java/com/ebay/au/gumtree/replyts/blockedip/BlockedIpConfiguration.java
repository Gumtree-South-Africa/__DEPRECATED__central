package com.ebay.au.gumtree.replyts.blockedip;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Properties;

@ComaasPlugin
@Configuration
public class BlockedIpConfiguration {

    @Bean
    public BlockedIpFilterFactory blockedIpFilterFactory(@Qualifier("blockedIpDataSource") DataSource dataSource) {
        return new BlockedIpFilterFactory(dataSource);
    }

    @Bean(destroyMethod = "close")
    public HikariDataSource blockedIpDataSource(
            @Value("${replyts2-blockedipfilter-plugin.dataSource.cachePrepStmts:true}") String cachePreparedStatements,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.prepStmtCacheSize:250}") String prepareStatementsCacheSize,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.prepStmtCacheSqlLimit:2048}") String prepareStatementCacheSqlLimit,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.useServerPrepStmts:true}") String useServerPrepareStatements,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.socketTimeout:60000}") String socketTimeout,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.connectTimeout:30000}") String connectionTimeout,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.url}") String datasourceUrl,
            @Value("${replyts2-blockedipfilter-plugin.username:belen}") String username,
            @Value("${replyts2-blockedipfilter-plugin.password:}") String password,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.pool.maxPoolSize:100}") int maxPoolSize,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.pool.minIdle:5}") int minIdle,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.borrowTimeout:2000}") int borrowTimeout,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.pool.name:}") String poolName) {

        Properties properties = new Properties();
        properties.setProperty("cachePrepStmts", cachePreparedStatements);
        properties.setProperty("prepStmtCacheSize", prepareStatementsCacheSize);
        properties.setProperty("prepStmtCacheSqlLimit", prepareStatementCacheSqlLimit);
        properties.setProperty("useServerPrepStmts", useServerPrepareStatements);
        properties.setProperty("socketTimeout", socketTimeout);
        properties.setProperty("connectTimeout", connectionTimeout);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(datasourceUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(maxPoolSize);
        dataSource.setMinimumIdle(minIdle);
        dataSource.setConnectionTimeout(borrowTimeout);
        dataSource.setPoolName(poolName);
        dataSource.setDataSourceProperties(properties);
        return dataSource;
    }
}
