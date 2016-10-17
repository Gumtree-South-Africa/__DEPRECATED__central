package com.ecg.replyts.core.runtime.identifier;

import org.junit.Test;

import static com.ecg.replyts.core.runtime.identifier.UserIdentifierType.BY_MAIL;
import static com.ecg.replyts.core.runtime.identifier.UserIdentifierType.BY_USER_ID;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class UserIdentifierServiceFactoryTest {

    @Test
    public void testCreateUserIdentifierServiceByMailAddress() throws Exception {

        UserIdentifierServiceFactory userIdentifierServiceFactory = new UserIdentifierServiceFactory(BY_MAIL, "some tenant");
        assertThat(userIdentifierServiceFactory.createUserIdentifierService(), instanceOf(UserIdentifierServiceByMailAddress.class));
    }

    @Test
    public void testCreateUserIdentifierServiceByUserIdHeadersMp() throws Exception {
        UserIdentifierServiceFactory userIdentifierServiceFactory = new UserIdentifierServiceFactory(BY_USER_ID, "mp");
        assertThat(userIdentifierServiceFactory.createUserIdentifierService(), instanceOf(UserIdentifierServiceByUserIdHeaders.class));
    }

    @Test
    public void testCreateUserIdentifierServiceByUserIdHeadersNotMp() throws Exception {
        UserIdentifierServiceFactory userIdentifierServiceFactory = new UserIdentifierServiceFactory(BY_USER_ID, "some tenant");
        assertThat(userIdentifierServiceFactory.createUserIdentifierService(), instanceOf(UserIdentifierServiceByUserIdHeaders.class));
    }
}