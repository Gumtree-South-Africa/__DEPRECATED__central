package com.ecg.replyts.core.runtime.identifier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = UserIdentifierConfigurationByMailAddressTest.TestContext.class)
@TestPropertySource(properties = {
  "tenant = some_tenant",
  "messagebox.userid.userIdentifierStrategy = BY_MAIL"
})
public class UserIdentifierConfigurationByMailAddressTest {
    @Autowired
    private UserIdentifierService identifierService;

    @Test
    public void testCreateUserIdentifierServiceByMailAddress() throws Exception {
        assertThat(identifierService, instanceOf(UserIdentifierServiceByMailAddress.class));
    }

    @Configuration
    @Import(UserIdentifierConfiguration.class)
    static class TestContext {
    }
}