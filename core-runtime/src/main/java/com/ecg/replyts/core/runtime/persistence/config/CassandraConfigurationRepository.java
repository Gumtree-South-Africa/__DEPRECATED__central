package com.ecg.replyts.core.runtime.persistence.config;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.PersistenceException;
import com.ecg.replyts.core.runtime.persistence.ValueSizeConstraint;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stores configurations in Cassandra.
 */
public class CassandraConfigurationRepository implements ConfigurationRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraConfigurationRepository.class);

    private static final String CONFIGURATIONS_KEY = "config";
    private static final ConfigurationJsonSerializer serializer = new ConfigurationJsonSerializer();

    private final Session session;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;
    private final Map<Statements, PreparedStatement> preparedStatements;
    private final ValueSizeConstraint sizeConstraint;

    private final Timer fetchTimer = TimingReports.newTimer("cassandra.configurationRepo-fetch");
    private final Timer persistTimer = TimingReports.newTimer("cassandra.configurationRepo-persist");

    public CassandraConfigurationRepository(ValueSizeConstraint sizeConstraint, Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this.sizeConstraint = sizeConstraint;
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        this.preparedStatements = Statements.prepare(session);
    }

    public CassandraConfigurationRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this(ValueSizeConstraint.maxMb(30), session, readConsistency, writeConsistency);
    }

    @Override
    public List<PluginConfiguration> getConfigurations() {
        return fetchConfigurations()
                .getConfigurationObjects()
                .stream()
                .map(ConfigurationObject::getPluginConfiguration)
                .collect(Collectors.toList());
    }

    @Override
    public void persistConfiguration(PluginConfiguration configuration) {
        ConfigurationObject obj = new ConfigurationObject(System.currentTimeMillis(), configuration);
        Configurations mergedConfigurations = fetchConfigurations().addOrUpdate(obj);
        persistConfigurations("Could not store configuration identified by " + configuration.getId().toString(), mergedConfigurations);
    }

    @Override
    public void deleteConfiguration(ConfigurationId configurationId) {
        Configurations mergedConfigurations = fetchConfigurations().delete(configurationId);
        persistConfigurations("Could not delete configuration identified by " + configurationId, mergedConfigurations);
    }

    private Configurations fetchConfigurations() {
        try (Timer.Context ignored = fetchTimer.time()) {
            Statement bound = Statements.SELECT.bind(this, CONFIGURATIONS_KEY);
            ResultSet resultset = session.execute(bound);
            Row row = resultset.one();
            if (row == null) {
                LOG.warn(
                        "No Filter Configurations available - no message filtering will be performed. If you don't have any filter rules configured, this is normal, otherwise there might be a problem with accessing Cassandra.",
                        ConfigurationConverter.KEY);
                return Configurations.EMPTY_CONFIG_SET;
            }
            return serializer.toDomain(row.getString("configuration"));
        }
    }

    private void persistConfigurations(String operationDescription, Configurations mergedConfigurations) {
        String json = serializer.fromDomain(mergedConfigurations);
        sizeConstraint.validate("Configurations as JSON", json.length());

        try (Timer.Context ignored = persistTimer.time()) {
            Statement updateStatement = Statements.UPDATE.bind(this, json, CONFIGURATIONS_KEY);
            session.execute(updateStatement);

        } catch (Exception e) {
            throw new PersistenceException(operationDescription, e);
        }
    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    private enum Statements {
        SELECT("SELECT configuration FROM core_configuration WHERE configuration_key=?"),
        UPDATE("UPDATE core_configuration SET configuration=? WHERE configuration_key=?", true);

        private final String cql;
        private final boolean modifying;

        Statements(String cql) {
            this(cql, false);
        }

        Statements(String cql, boolean modifying) {
            this.cql = cql;
            this.modifying = modifying;
        }

        public static Map<Statements, PreparedStatement> prepare(Session session) {
            Map<Statements, PreparedStatement> result = new EnumMap<>(Statements.class);
            for (Statements statement : values()) {
                result.put(statement, session.prepare(statement.cql));
            }
            return result;
        }

        public Statement bind(CassandraConfigurationRepository repository, Object... values) {
            return repository.preparedStatements
                    .get(this)
                    .bind(values)
                    .setConsistencyLevel(getConsistencyLevel(repository))
                    .setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
        }

        private ConsistencyLevel getConsistencyLevel(CassandraConfigurationRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }
}
