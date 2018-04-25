package com.ebay.au.gumtree.replyts.blockedip;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@ComaasPlugin
@Configuration
@ComponentScan(basePackageClasses = BlockedIpConfiguration.class)
public class BlockedIpConfiguration {

    @Bean
    public JdbcTemplate blockedIpJdbcTemplate(
            @Value("${replyts2-blockedipfilter-plugin.dataSource.url}") String url,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.username:belen}") String username,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.password:}") String password,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.pool.maxPoolSize:50}") int maxPoolSize,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.pool.name:}") String poolName,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.cachePrepStmts:true}") String cachePreparedStatements,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.prepStmtCacheSize:250}") String prepareStatementsCacheSize,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.prepStmtCacheSqlLimit:2048}") String prepareStatementCacheSqlLimit,
            @Value("${replyts2-blockedipfilter-plugin.dataSource.useServerPrepStmts:true}") String useServerPrepareStatements
    ) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setPoolName(poolName);
        hikariConfig.setConnectionInitSql("SELECT 1");
        hikariConfig.addDataSourceProperty("cachePrepStmts", cachePreparedStatements);
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", prepareStatementsCacheSize);
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", prepareStatementCacheSqlLimit);
        hikariConfig.addDataSourceProperty("useServerPrepStmts", useServerPrepareStatements);
        return new JdbcTemplate(new HikariDataSource(hikariConfig));
    }
}
