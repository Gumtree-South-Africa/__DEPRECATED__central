package com.ecg.replyts.core.webapi.control.health;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DataSourceHealthCommand extends AbstractHealthCommand {

    private static final String DEFAULT_QUERY = "SELECT 1";
    static final String COMMAND_NAME = "datasource";

    private final List<NamedJdbcTemplate> jdbcTemplates;

    DataSourceHealthCommand(Map<String, DataSource> dataSources) {
        this.jdbcTemplates = dataSources.entrySet().stream()
                .map(NamedJdbcTemplate::new)
                .collect(Collectors.toList());
    }

    @Override
    public ObjectNode execute() {
        List<CompletableFuture<ObjectNode>> processingFutures = jdbcTemplates.stream()
                .map(DataSourceHealthCommand::checkDataSourceSupplier)
                .map(CompletableFuture::supplyAsync)
                .collect(Collectors.toList());

        CompletableFuture[] futuresArray = processingFutures.toArray(new CompletableFuture[processingFutures.size()]);
        CompletableFuture.allOf(futuresArray).join();

        ArrayNode connections = processingFutures.stream()
                .map(future -> future.getNow(JsonObjects.newJsonObject()))
                .collect(ARRAY_NODE_COLLECTOR);

        return JsonObjects.builder()
                .attr("connections", connections)
                .attr("status", getOverallStatus(connections).name())
                .build();
    }

    private static Supplier<ObjectNode> checkDataSourceSupplier(NamedJdbcTemplate namedJdbcTemplate) {
        return () -> checkDataSource(namedJdbcTemplate);
    }

    private static ObjectNode checkDataSource(NamedJdbcTemplate namedJdbcTemplate) {
        try {
            List<Status> results = namedJdbcTemplate.jdbcTemplate.query(DEFAULT_QUERY, new SingleColumnRowMapper());
            if (results.size() == 0) {
                return status(namedJdbcTemplate.name, Status.DOWN, "Empty response from datasource.");
            }

            return status(namedJdbcTemplate.name, Status.UP);
        } catch (Exception ex) {
            return status(namedJdbcTemplate.name, Status.DOWN, ex.getMessage());
        }
    }

    @Override
    public String name() {
        return COMMAND_NAME;
    }

    /**
     * {@link RowMapper} that expects and returns results from a single column.
     */
    private static class SingleColumnRowMapper implements RowMapper<Status> {

        @Override
        public Status mapRow(ResultSet rs, int rowNum) throws SQLException {
            ResultSetMetaData metaData = rs.getMetaData();
            int columns = metaData.getColumnCount();
            if (columns != 1) {
                return Status.DOWN;
            }
            return Status.UP;
        }
    }

    private static class NamedJdbcTemplate {
        private final String name;
        private final JdbcTemplate jdbcTemplate;

        private NamedJdbcTemplate(Map.Entry<String, DataSource> entry) {
            this.name = entry.getKey();
            this.jdbcTemplate = new JdbcTemplate(entry.getValue());
        }
    }
}
