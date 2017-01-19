package com.ecg.de.kleinanzeigen.replyts.belen.blockeduser;

import org.springframework.jdbc.core.JdbcTemplate;

class UserDataStateResolver {

    private final ScalarQuery query;

    UserDataStateResolver(JdbcTemplate tpl) {
        this.query = new ScalarQuery(tpl);
    }

    public UserState resolve(String email) {

        String state = query.query("select status from userdata where email=?", email);

        if (state == null) {
            return UserState.UNDECIDED;
        } else if (state.equalsIgnoreCase("ACTIVE")) {
            return UserState.ACTIVE;
        } else if (state.equalsIgnoreCase("BLOCKED")) {
            return UserState.BLOCKED;
        }
        return UserState.UNDECIDED;

    }

}
