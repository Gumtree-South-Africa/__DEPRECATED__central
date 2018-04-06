package com.ecg.comaas.core.filter.belenblockeduser;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import java.beans.PropertyVetoException;

@ComaasPlugin
@Configuration
@ComponentScan(basePackageClasses = BlockedUserFilterConfiguration.class)
public class BlockedUserFilterConfiguration {

    @Bean(destroyMethod = "close")
    public DataSource userDataSource(
        @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.url}") String jdbcUrl,
        @Value("${replyts2-belenblockeduserfilter-plugin.username:belen}") String user,
        @Value("${replyts2-belenblockeduserfilter-plugin.password:}") String password,
        @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.initialPoolSize:5}") int initialPoolSize,
        @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.minPoolSize:3}") int minPoolSize,
        @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.maxPoolSize:100}") int maxPoolSize,
        @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.acquireIncrement:5}") int acquireIncrement,
        @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.maxIdleTime:90}") int maxIdleTime,
        @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.maxConnectionAge:900}") int maxConnectionAge,
        @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.idleConnectionTestPeriod:30}") int idleConnectionTestPeriod,
        @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.name:belen_user}") String dataSourceName,
        @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.checkoutTimeout:2000}") int checkoutTimeout,
        @Value("${replyts2-belenblockeduserfilter-plugin.dataSource.pool.c3p0.numHelperThreads:10}") int numHelperThreads) throws PropertyVetoException {

        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass(com.mysql.jdbc.Driver.class.getName());
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        dataSource.setInitialPoolSize(initialPoolSize);
        dataSource.setMinPoolSize(minPoolSize);
        dataSource.setMaxPoolSize(maxPoolSize);
        dataSource.setAcquireIncrement(acquireIncrement);
        dataSource.setMaxIdleTime(maxIdleTime);
        dataSource.setMaxConnectionAge(maxConnectionAge);
        dataSource.setIdleConnectionTestPeriod(idleConnectionTestPeriod);
        dataSource.setDataSourceName(dataSourceName);
        dataSource.setCheckoutTimeout(checkoutTimeout);
        dataSource.setConnectionTesterClassName(com.mysql.jdbc.integration.c3p0.MysqlConnectionTester.class.getName());
        dataSource.setNumHelperThreads(numHelperThreads);

        return dataSource;
    }
}
