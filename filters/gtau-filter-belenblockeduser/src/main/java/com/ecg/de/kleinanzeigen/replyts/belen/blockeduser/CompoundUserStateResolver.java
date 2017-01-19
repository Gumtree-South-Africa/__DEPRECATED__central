package com.ecg.de.kleinanzeigen.replyts.belen.blockeduser;

import org.springframework.jdbc.core.JdbcTemplate;

class CompoundUserStateResolver {

    private final UserDataStateResolver userDataStateResolver;

    CompoundUserStateResolver(JdbcTemplate tpl) {
        this(new UserDataStateResolver(tpl));
    }

    CompoundUserStateResolver(UserDataStateResolver userDataStateResolver) {
        this.userDataStateResolver = userDataStateResolver;
    }

    public UserState resolve(String email) {
        return userDataStateResolver.resolve(email);
    }
}
