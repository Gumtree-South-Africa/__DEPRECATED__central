package com.ecg.comaas.gtau.filter.belenblockeduser;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Properties;

@ComaasPlugin
@Configuration
public class BlockedUserConfiguration {

    @Bean
    public BlockedUserFilterFactory blockedUserFilterFactory(@Qualifier("blockedUsersDataSource") DataSource dataSource) {
        return new BlockedUserFilterFactory(dataSource);
    }

    @Bean(destroyMethod = "close")
    public HikariDataSource blockedUsersDataSource(
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.cachePrepStmts:true}") String cachePreparedStatements,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.prepStmtCacheSize:250}") String prepareStatementsCacheSize,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.prepStmtCacheSqlLimit:2048}") String prepareStatementCacheSqlLimit,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.useServerPrepStmts:true}") String useServerPrepareStatements,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.socketTimeout:60000}") String socketTimeout,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.connectTimeout:30000}") String connectionTimeout,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.url}") String datasourceUrl,
            @Value("${replyts2-belenblockeduserfilter-plugin.username:belen}") String username,
            @Value("${replyts2-belenblockeduserfilter-plugin.password:}") String password,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.maxPoolSize:100}") int maxPoolSize,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.minIdle:5}") int minIdle,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.borrowTimeout:2000}") int borrowTimeout,
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.name:}") String poolName) {

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

    @Bean
    public MethodInvokingFactoryBean methodInvokingFactoryBean(
            @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.loginTimeout:1}") int loginTimeout) {
        MethodInvokingFactoryBean bean = new MethodInvokingFactoryBean();
        bean.setArguments(new Object[]{loginTimeout});
        bean.setStaticMethod("java.sql.DriverManager.setLoginTimeout");
        return bean;
    }
}
