package ca.kijiji.replyts;

import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(JMockit.class)
public class LeGridClientTest {

    @Tested
    private LeGridClient leGridClient;

    @Before
    public void setUp() throws Exception {
        leGridClient = new LeGridClient("http://legrid/api", "user", "password");
    }

    @Test
    public void testInitialization() throws Exception {
        assertThat(leGridClient.getGridApiEndPoint(), is("http://legrid/api"));
        assertNotNull(leGridClient.getClient());
    }

}