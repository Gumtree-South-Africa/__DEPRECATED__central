package com.ecg.comaas.core.filter.belenblockeduser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static com.ecg.replyts.core.runtime.prometheus.PrometheusFailureHandler.reportExternalServiceFailure;

public class StateDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(StateDataSource.class);
    private static final String SELECT_STATUS_FROM_USER_DATA = "SELECT status FROM userdata WHERE email=?";
    private static final String SELECT_STATUS_EXTERNAL_USER_TNS = "SELECT status FROM external_user_tns WHERE email=?";

    private final JdbcTemplate jdbcTemplate;
    private final boolean extTnsEnabled;

    public StateDataSource(JdbcTemplate jdbcTemplate, boolean extTnsEnabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.extTnsEnabled = extTnsEnabled;
    }

    public UserState getState(String email) {
        UserState userDataState = getFromUserData(email);
        if (extTnsEnabled && UserState.UNDECIDED == userDataState) {
            return getFromExternalTnS(email);
        }
        return userDataState;
    }

    private UserState getFromUserData(String email) {
        String state = getQueryResult(SELECT_STATUS_FROM_USER_DATA, email);
        UserState userState = UserState.from(state, UserState.UNDECIDED);
        LOG.trace("User state result from user_data table for email {} is {}", email, userState);
        return userState;
    }

    private UserState getFromExternalTnS(String email) {
        String state = getQueryResult(SELECT_STATUS_EXTERNAL_USER_TNS, email);
        UserState userState = UserState.from(state, UserState.ACTIVE);
        LOG.trace("User state result from external_tns for email {} is {}", email, userState);
        return UserState.from(state, UserState.ACTIVE);
    }

    private String getQueryResult(String sql, String email) {
        try {
            List<String> resultSet = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), email);
            if (resultSet.isEmpty()) {
                return null;
            }
            return resultSet.get(0);
        } catch (DataAccessException dae) {
            // can be thrown by Spring JDBC Template in case of any DB related issues
            reportExternalServiceFailure("mysql_get_user_state");
            LOG.error("Query '{}' with param '{}' failed", sql, email, dae);
            return null;
        }
    }
}
