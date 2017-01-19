package com.ecg.de.kleinanzeigen.replyts.belen.blockeduser;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ScalarQuery {

    private final JdbcTemplate tpl;

    public ScalarQuery(JdbcTemplate tpl) {
        this.tpl = tpl;
    }

    public String query(String sql, String...placeholders) {
        List<String> resultSet = tpl.query(sql, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(1);
            }
        }, placeholders);

        if(resultSet.isEmpty()) {
            return null;
        }
        return resultSet.get(0);
    }
}
