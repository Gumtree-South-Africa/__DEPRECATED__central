package com.ecg.comaas.core.filter.belenblockeduser;

import org.springframework.jdbc.core.JdbcTemplate;

class ExternalUserTnsStateResolver {

    private final ScalarQuery query;

    ExternalUserTnsStateResolver(JdbcTemplate tpl) {
        this.query = new ScalarQuery(tpl);
    }

    public UserState resolve(String email) {

        String state = query.query("select status from external_user_tns where email=?", email);

        if (state == null) {
            return UserState.ACTIVE;
        } else if (state.equalsIgnoreCase("BLOCKED")) {
            return UserState.BLOCKED;
        }
        return UserState.ACTIVE;

    }

}
