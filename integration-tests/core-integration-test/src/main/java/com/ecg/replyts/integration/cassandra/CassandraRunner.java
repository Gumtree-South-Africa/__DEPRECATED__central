package com.ecg.replyts.integration.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.ecg.replyts.util.CassandraTestUtil;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.DataLoader;
import org.cassandraunit.dataset.ClassPathDataSet;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.spring.*;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
* This is essentially a copy of @AbstractCassandraUnitTestExecutionListener with one important difference
 * Cassandra Cluster is initialized with
 * .withMaxSchemaAgreementWaitSeconds(1) and .withQueryOptions(CassandraTestUtil.CLUSTER_OPTIONS)
 * This is to reduce time it takes to create a new schema in the keyspace
 * There does not seem to be a better way of providing this simple customization option
 */
public class CassandraRunner extends AbstractTestExecutionListener implements Ordered {

        private static boolean initialized = false;
        private static final Logger LOGGER = LoggerFactory.getLogger(CassandraUnitDependencyInjectionTestExecutionListener.class);

        @Override
        public void beforeTestClass(TestContext testContext) throws Exception {
            startServer(testContext);
        }

        @Override
        public void afterTestMethod(TestContext testContext) throws Exception {
            if (Boolean.TRUE.equals(testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Cleaning and reloading server for test context [" + testContext + "].");
                }
                cleanServer();
                startServer(testContext);
            }
        }

        @Override
        public void afterTestClass(TestContext testContext) throws Exception {
            cleanServer();
        }

    protected void startServer(TestContext testContext) throws Exception {
        EmbeddedCassandra embeddedCassandra = Preconditions.checkNotNull(
                AnnotationUtils.findAnnotation(testContext.getTestClass(), EmbeddedCassandra.class),
                "CassandraUnitTestExecutionListener must be used with @EmbeddedCassandra on " + testContext.getTestClass());
        if (!initialized) {
            String yamlFile = Optional.fromNullable(embeddedCassandra.configuration()).get();
            long timeout = embeddedCassandra.timeout();
            EmbeddedCassandraServerHelper.startEmbeddedCassandra(yamlFile, timeout);
            initialized = true;
        }

        String clusterName = EmbeddedCassandraServerHelper.getClusterName();
        String host = EmbeddedCassandraServerHelper.getHost();
        int rpcPort = EmbeddedCassandraServerHelper.getRpcPort();
        int nativeTransportPort = EmbeddedCassandraServerHelper.getNativeTransportPort();

        CassandraDataSet cassandraDataSet = AnnotationUtils.findAnnotation(testContext.getTestClass(), CassandraDataSet.class);
        if (cassandraDataSet != null) {
            List<String> dataset = null;
            ListIterator<String> datasetIterator = null;
            String keyspace = cassandraDataSet.keyspace();
            // TODO : find a way to hide them and avoid switch, need some refactoring cassandra-unit
            switch (cassandraDataSet.type()) {
                case cql:
                    dataset = dataSetLocations(testContext, cassandraDataSet);
                    datasetIterator = dataset.listIterator();
                    // Cluster init change is here
                    Cluster cluster = new Cluster.Builder().addContactPoints(host)
                            .withPort(nativeTransportPort)
                            .withMaxSchemaAgreementWaitSeconds(1)
                            .withQueryOptions(CassandraTestUtil.CLUSTER_OPTIONS)
                            .build();
                    Session session = cluster.connect();

                    CQLDataLoader cqlDataLoader = new CQLDataLoader(session);
                    while (datasetIterator.hasNext()) {
                        String next = datasetIterator.next();
                        boolean dropAndCreateKeyspace = datasetIterator.previousIndex() == 0;
                        cqlDataLoader.load(new ClassPathCQLDataSet(next, dropAndCreateKeyspace, dropAndCreateKeyspace, keyspace));
                    }
                    break;
                default:
                    dataset = dataSetLocations(testContext, cassandraDataSet);
                    datasetIterator = dataset.listIterator();
                    DataLoader dataLoader = new DataLoader(clusterName, host + ":" + rpcPort);
                    while (datasetIterator.hasNext()) {
                        String next = datasetIterator.next();
                        boolean dropAndCreateKeyspace = datasetIterator.previousIndex() == 0;
                        dataLoader.load(new ClassPathDataSet(next), dropAndCreateKeyspace);
                    }
            }
        }

    }

    private List<String> dataSetLocations(TestContext testContext, CassandraDataSet cassandraDataSet) {
        String[] dataset = cassandraDataSet.value();
        if (dataset.length == 0) {
            String alternativePath = alternativePath(testContext.getTestClass(), true, cassandraDataSet.type().name());
            if (testContext.getApplicationContext().getResource(alternativePath).exists()) {
                dataset = new String[]{alternativePath.replace(ResourceUtils.CLASSPATH_URL_PREFIX + "/", "")};
            } else {
                alternativePath = alternativePath(testContext.getTestClass(), false, cassandraDataSet.type().name());
                if (testContext.getApplicationContext().getResource(alternativePath).exists()) {
                    dataset = new String[]{alternativePath.replace(ResourceUtils.CLASSPATH_URL_PREFIX + "/", "")};
                } else {
                    LOGGER.info("No dataset will be loaded");
                }
            }
        }
        return Arrays.asList(dataset);
    }

    protected void cleanServer() {
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    protected String alternativePath(Class<?> clazz, boolean includedPackageName, String extension) {
        if (includedPackageName) {
            return ResourceUtils.CLASSPATH_URL_PREFIX + "/" + ClassUtils.convertClassNameToResourcePath(clazz.getName()) + "-dataset" + "." + extension;
        } else {
            return ResourceUtils.CLASSPATH_URL_PREFIX + "/" + clazz.getSimpleName() + "-dataset" + "." + extension;
        }
    }

    @Override
    public int getOrder() {
        // since spring 4.1 the default-order is LOWEST_PRECEDENCE. But we want to start EmbeddedCassandra even before
        // the springcontext such that beans can connect to the started cassandra
        return 0;
    }
}
