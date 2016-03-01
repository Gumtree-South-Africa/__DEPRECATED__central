package com.ecg.de.kleinanzeigen.replyts.belen.blockeduser;

import org.springframework.jdbc.core.JdbcTemplate;

class CompoundUserStateResolver {

    private final UserDataStateResolver userDataStateResolver;
    private final ExternalUserTnsStateResolver externalUserTnsStateResolver;

    CompoundUserStateResolver(JdbcTemplate tpl) {
        this(new UserDataStateResolver(tpl), new ExternalUserTnsStateResolver(tpl));
    }

    CompoundUserStateResolver(UserDataStateResolver userDataStateResolver, ExternalUserTnsStateResolver externalUserTnsStateResolver) {
        this.userDataStateResolver = userDataStateResolver;
        this.externalUserTnsStateResolver = externalUserTnsStateResolver;
    }

    public UserState resolve(String email) {
        UserState userDataState = userDataStateResolver.resolve(email);
        if(userDataState==UserState.UNDECIDED) {
            return externalUserTnsStateResolver.resolve(email);
        }
        return userDataState;
    }
}
