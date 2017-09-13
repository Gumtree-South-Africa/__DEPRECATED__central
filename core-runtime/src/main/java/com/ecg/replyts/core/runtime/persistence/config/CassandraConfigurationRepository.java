package com.ecg.replyts.core.runtime.persistence.config;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.datastax.driver.core.utils.UUIDs;
import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.cluster.XidFactory;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.ecg.replyts.core.runtime.persistence.PersistenceException;
import com.ecg.replyts.core.runtime.persistence.ValueSizeConstraint;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stores configurations in Cassandra.
 */
public class CassandraConfigurationRepository implements ConfigurationRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraConfigurationRepository.class);

    private static final String CURRENT_VERSION_ID = "current";

    private final Session session;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;
    private final Map<Statements, PreparedStatement> preparedStatements;
    private final ValueSizeConstraint sizeConstraint;

    private final Timer fetchTimer = TimingReports.newTimer("cassandra.configurationRepo-fetch");
    private final Timer persistTimer = TimingReports.newTimer("cassandra.configurationRepo-store");

    public CassandraConfigurationRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this.sizeConstraint = ValueSizeConstraint.maxMb(30);
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        this.preparedStatements = Statements.prepare(session);
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
    public void persistConfiguration(PluginConfiguration configuration, String remoteAddress) {
        ConfigurationObject obj = new ConfigurationObject(System.currentTimeMillis(), configuration);
        Configurations mergedConfigurations = fetchConfigurations().addOrUpdate(obj);
        persistConfigurations(configuration.getId().toString(), mergedConfigurations, remoteAddress);
    }

    @Override
    public void deleteConfiguration(String pluginFactory, String instanceId, String remoteAddress) {
        final ConfigurationId configurationId = new ConfigurationId(pluginFactory, instanceId);
        Configurations mergedConfigurations = fetchConfigurations().delete(configurationId);
        persistConfigurations("Could not delete configuration identified by " + configurationId, mergedConfigurations, remoteAddress);
    }

    @Override
    public void replaceConfigurations(List<PluginConfiguration> pluginConfigurations, String remoteAddress) {
        long currentTimeMillis = System.currentTimeMillis();
        List<ConfigurationObject> configurationObjects = Lists.transform(pluginConfigurations, c -> new ConfigurationObject(currentTimeMillis, c));
        Configurations configurations = new Configurations(configurationObjects);
        persistConfigurations("Could not store configurations", configurations, remoteAddress);
    }

    private Configurations fetchConfigurations() {
        try (Timer.Context ignored = fetchTimer.time()) {
            return fetchAuditedConfiguration().orElseGet(() -> {
                LOG.info("Could not find audited configuration, falling back to old table.");
                Row row = session.execute(Statements.SELECT.bind(this)).one();
                if (row == null) {
                    LOG.warn("No Filter Configurations available - no message filtering will be performed. If you don't have any " +
                                    "filter rules configured, this is normal, otherwise there might be a problem with accessing Cassandra.",
                            ConfigurationConverter.KEY);
                    return Configurations.EMPTY_CONFIG_SET;
                }
                return ConfigurationJsonSerializer.toDomain(row.getString("configuration"));
            });
        }
    }

    private Optional<Configurations> fetchAuditedConfiguration() {
        Row row = session.execute(Statements.SELECT_CURRENT_INDEX.bind(this)).one();
        if (row == null) {
            return Optional.empty();
        }
        UUID logId = row.getUUID("log_id");
        Statement stmt = Statements.SELECT_LOG_RECORD.bind(this, logId);
        return Optional.ofNullable(session.execute(stmt).one())
                .map(r -> ConfigurationJsonSerializer.toDomain(r.getString("configuration")));
    }

    private void persistConfigurations(final String configurationId, final Configurations mergedConfigurations, final String remoteAddress) {
        String json = ConfigurationJsonSerializer.fromDomain(mergedConfigurations);
        sizeConstraint.validate("Configurations as JSON", json.length());

        try (Timer.Context ignored = persistTimer.time()) {
            // Save to both tables for now
            UUID logId = UUIDs.timeBased();
            session.execute(new BatchStatement()
                    .add(Statements.INSERT_LOG_RECORD.bind(this, logId, json, remoteAddress, new Date(), MDC.get(MDCConstants.CORRELATION_ID)))
                    .add(Statements.UPSERT_INDEX.bind(this, logId, CURRENT_VERSION_ID))
                    .add(Statements.UPSERT_INDEX.bind(this, logId, XidFactory.nextXid()))
                    .add(Statements.UPDATE.bind(this, json))
                    .setConsistencyLevel(getWriteConsistency())
            );
            LOG.info("current configuration logId is now {}", logId);
        } catch (Exception e) {
            throw new PersistenceException("Could not store configuration identified by " + configurationId, e);
        }
    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    private enum Statements {
        // These are for the old, single row table "core_configuration". Can be removed once all tenants are on the new version.
        SELECT("SELECT configuration FROM core_configuration WHERE configuration_key = 'config'"),
        UPDATE("UPDATE core_configuration SET configuration=? WHERE configuration_key = 'config'", true),

        // These are for the new "audited" table, which saves previous versions of the configuration.
        INSERT_LOG_RECORD("INSERT INTO core_configuration_log (log_id, configuration, remote_address, received_time, correlation_id) VALUES (?, ?, ?, ?, ?)", true),
        SELECT_LOG_RECORD("SELECT configuration FROM core_configuration_log WHERE log_id = ?"),
        UPSERT_INDEX("UPDATE core_configuration_index set log_id = ? WHERE version = ?", true),
        SELECT_CURRENT_INDEX("SELECT log_id FROM core_configuration_index WHERE version = '" + CURRENT_VERSION_ID + "'");

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
                    .setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL)
                    .setIdempotent(!modifying);
        }

        private ConsistencyLevel getConsistencyLevel(CassandraConfigurationRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }
}
