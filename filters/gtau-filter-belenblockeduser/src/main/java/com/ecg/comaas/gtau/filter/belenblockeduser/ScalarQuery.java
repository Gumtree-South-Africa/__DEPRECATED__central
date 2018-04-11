package com.ecg.comaas.gtau.filter.belenblockeduser;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

class ScalarQuery {

    private final JdbcTemplate tpl;

    ScalarQuery(JdbcTemplate tpl) {
        this.tpl = tpl;
    }

    String query(String sql, String... placeholders) {
        List<String> resultSet = tpl.query(sql, (rs, rowNum) -> rs.getString(1), placeholders);
        if (resultSet.isEmpty()) {
            return null;
        }
        return resultSet.get(0);
    }
}
