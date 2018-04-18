package com.ecg.comaas.core.filter.belenblockeduser;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@ComaasPlugin
@Configuration
@ComponentScan(basePackageClasses = BlockedUserFilterConfiguration.class)
public class BlockedUserFilterConfiguration {

    @Bean
    public JdbcTemplate belenBlockedUserJdbcTemplate(
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.url}") String url,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.username:belen}") String username,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.password:}") String password,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.initialPoolSize:5}") int initialPoolSize,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.maxPoolSize:100}") int maxPoolSize,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.name:belen_user}") String dataSourceName,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.maxIdleTime:90}") int maxIdleTime,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.connectTimeout:30000}") long connectionTimeout,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.cachePrepStmts:true}") String cachePreparedStatements,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.prepStmtCacheSize:250}") String prepareStatementsCacheSize,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.prepStmtCacheSqlLimit:2048}") String prepareStatementCacheSqlLimit,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.useServerPrepStmts:true}") String useServerPrepareStatements,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.socketTimeout:60000}") String socketTimeout
    ) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMinimumIdle(initialPoolSize);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setPoolName(dataSourceName);
        hikariConfig.setIdleTimeout(maxIdleTime);
        hikariConfig.setConnectionTimeout(connectionTimeout);
        hikariConfig.addDataSourceProperty("cachePrepStmts", cachePreparedStatements);
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", prepareStatementsCacheSize);
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", prepareStatementCacheSqlLimit);
        hikariConfig.addDataSourceProperty("useServerPrepStmts", useServerPrepareStatements);
        hikariConfig.addDataSourceProperty("socketTimeout", socketTimeout);
        return new JdbcTemplate(new HikariDataSource(hikariConfig));
    }

    @Bean
    public MethodInvokingFactoryBean methodInvokingFactoryBean(@Value("${replyts2-belenblockeduserfilter-plugin.dataSource.loginTimeout:1}") int loginTimeout) {
        MethodInvokingFactoryBean bean = new MethodInvokingFactoryBean();
        bean.setArguments(new Object[]{loginTimeout});
        bean.setStaticMethod("java.sql.DriverManager.setLoginTimeout");
        return bean;
    }
}
