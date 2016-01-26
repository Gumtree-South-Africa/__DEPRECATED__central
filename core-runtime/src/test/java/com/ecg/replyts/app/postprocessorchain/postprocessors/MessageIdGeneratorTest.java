package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.core.runtime.mailparser.MessageIdHeaderEncryption;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * User: gdibella
 * Date: 9/3/13
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageIdGeneratorTest {
    @Before
    public void setUp() throws Exception {
        when(encryption.encrypt(anyString())).thenReturn("12344");

    }

    @Mock
    private MessageIdHeaderEncryption encryption;

    @Test
    public void testWithSingleDomain() throws Exception {
        assertThat(new MessageIdGenerator(new String[]{"kijiji.it"}, encryption).encryptedMessageId("1"), equalTo("<12344@kijiji.it>"));
    }

    @Test
    public void testWithMultiDomain() throws Exception {
        assertThat(new MessageIdGenerator(new String[]{"kijiji.it", "ebay.de"}, encryption).encryptedMessageId("1"), equalTo("<12344@kijiji.it>"));
    }
}
