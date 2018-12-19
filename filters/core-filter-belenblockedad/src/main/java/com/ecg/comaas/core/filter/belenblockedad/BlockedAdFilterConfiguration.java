package com.ecg.comaas.core.filter.belenblockedad;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;

@ComaasPlugin
@Profile({TENANT_GTAU, TENANT_EBAYK})
@Configuration
@ComponentScan(basePackageClasses = BlockedAdFilterConfiguration.class)
public class BlockedAdFilterConfiguration {

    @Bean(destroyMethod = "close")
    public DataSource adDataSource(
        @Value("${replyts2-belenblockedadfilter-plugin.dataSource.url}") String jdbcUrl,
        @Value("${replyts2-belenblockedadfilter-plugin.username:belen}") String user,
        @Value("${replyts2-belenblockedadfilter-plugin.password:}") String password,
        @Value("${replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.initialPoolSize:5}") int initialPoolSize,
        @Value("${replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.minPoolSize:3}") int minPoolSize,
        @Value("${replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.maxPoolSize:4}") int maxPoolSize,
        @Value("${replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.acquireIncrement:1}") int acquireIncrement,
        @Value("${replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.maxIdleTime:90}") int maxIdleTime,
        @Value("${replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.maxConnectionAge:900}") int maxConnectionAge,
        @Value("${replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.idleConnectionTestPeriod:30}") int idleConnectionTestPeriod,
        @Value("${replyts2-belenblockedadfilter-plugin.dataSource.name:belen_ad}") String dataSourceName,
        @Value("${replyts2-belenblockedadfilter-plugin.dataSource.pool.checkoutTimeout:2000}") int checkoutTimeout,
        @Value("${replyts2-belenblockedadfilter-plugin.dataSource.pool.c3p0.numHelperThreads:10}") int numHelperThreads) throws PropertyVetoException {

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
