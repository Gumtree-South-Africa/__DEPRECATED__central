package com.ecg.messagecenter.identifier;

import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class UserIdentifierServiceFactoryTest {

    @Test
    public void testCreateUserIdentifierServiceByMailAddress() throws Exception {
        UserIdentifierServiceFactory userIdentifierServiceFactory = new UserIdentifierServiceFactory(UserIdentifierType.BY_MAIL, "user-id-buyer", "user-id-seller");
        assertThat(userIdentifierServiceFactory.createUserIdentifierService(), instanceOf(UserIdentifierServiceByMailAddress.class));
    }

    @Test
    public void testCreateUserIdentifierServiceByUserIdHeaders() throws Exception {
        UserIdentifierServiceFactory userIdentifierServiceFactory = new UserIdentifierServiceFactory(UserIdentifierType.BY_USER_ID, "user-id-buyer", "user-id-seller");
        assertThat(userIdentifierServiceFactory.createUserIdentifierService(), instanceOf(UserIdentifierServiceByUserIdHeaders.class));
    }
}