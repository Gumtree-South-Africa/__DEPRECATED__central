package com.ecg.replyts.core.runtime.persistence.clock;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.EnumMap;
import java.util.Map;

/**
 * {@link CronJobClockRepository} for cassandra.
 */
public class CassandraCronJobClockRepository implements CronJobClockRepository {

    private static final String FIELD_LAST_PROCESSED_DATE = "last_processed_date";

    private final Session session;
    private final Map<Statements, PreparedStatement> preparedStatements;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private final Timer setCronJobClockTimer = TimingReports.newTimer("cassandra.cronJobClockRepo-setClock");
    private final Timer getCronJobLastProcessedDateTimer = TimingReports.newTimer("cassandra.cronJobClockRepo-getLastProcessedDate");

    private ObjectMapper objectMapper;

    public CassandraCronJobClockRepository(Session session, ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        preparedStatements = Statements.prepare(session);
    }


    @Override
    public void set(String cronJobName, DateTime lastRunDate, DateTime lastProcessedDate) {
        try (Timer.Context ignored = setCronJobClockTimer.time()) {
            session.execute(Statements.INSERT_CRON_JOB_CLOCK.bind(this, cronJobName, lastRunDate.toDate(), lastProcessedDate.toDate()));
        }
    }

    @Override
    public DateTime getLastProcessedDate(String cronJobName) {
        try (Timer.Context ignored = getCronJobLastProcessedDateTimer.time()) {
            ResultSet result = session.execute(Statements.SELECT_LAST_PROCESSED_DATE.bind(this, cronJobName));
            Row row = result.one();
            if (row == null) {
                return null;
            }
            return new DateTime(row.getDate(FIELD_LAST_PROCESSED_DATE).getTime());
        }
    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    private enum Statements {

        SELECT_LAST_PROCESSED_DATE("SELECT last_processed_date FROM core_cronjob_clock WHERE job_name=? "),
        INSERT_CRON_JOB_CLOCK("INSERT INTO core_cronjob_clock (job_name, last_run_date, last_processed_date) VALUES (?,?,?)", true);

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
            Map<Statements, PreparedStatement> statements = new EnumMap<>(Statements.class);
            for (Statements statement : values()) {
                statements.put(statement, session.prepare(statement.cql));
            }
            return ImmutableMap.copyOf(statements);
        }

        public Statement bind(CassandraCronJobClockRepository repository, Object... values) {
            return repository.preparedStatements
                    .get(this)
                    .bind(values)
                    .setConsistencyLevel(getConsistencyLevel(repository))
                    .setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL);
        }

        private ConsistencyLevel getConsistencyLevel(CassandraCronJobClockRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }

    @Autowired
    public void setObjectMapperConfigurer(JacksonAwareObjectMapperConfigurer jacksonAwareObjectMapperConfigurer) {
        this.objectMapper = jacksonAwareObjectMapperConfigurer.getObjectMapper();
    }
}
