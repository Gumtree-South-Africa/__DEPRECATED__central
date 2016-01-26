package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.cluster.Guids;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NewConversationCreatorTest {

    @Mock
    private MutableMail mail;

    @Mock
    private Guids guids;


    private MessageProcessingContext context;
    @Mock
    private NewConversationCreator creator;

    @Mock
    private UniqueConversationSecret convSecret;

    @Mock
    private ProcessingTimeGuard processingTimeGuard;

    @Before
    public void setUp() throws Exception {
        when(convSecret.nextSecret()).thenReturn("secretofbuyer", "secretofseller");
        when(guids.nextGuid()).thenReturn("foobar@foobar");
        when(mail.getAdId()).thenReturn("332211");

        context = new MessageProcessingContext(mail, "1", processingTimeGuard);
        creator = new NewConversationCreator(guids, convSecret);
    }

    @Test
    public void prefersReplyToOverFromHeaderWhenChoosingSender() throws Exception {
        when(mail.getDeliveredTo()).thenReturn("to@host.com");
        when(mail.getFrom()).thenReturn("from@host.com");
        when(mail.getReplyTo()).thenReturn("replyto@host.com");
        creator.setupNewConversation(context);

        assertThat(context.getConversation().getBuyerId(), is("replyto@host.com"));

    }

    @Test
    public void createsMessageContextProperly() {
        when(mail.getDeliveredTo()).thenReturn("to@host.com");
        when(mail.getFrom()).thenReturn("from@host.com");

        creator.setupNewConversation(context);

        assertEquals("332211", context.getConversation().getAdId());
        assertEquals(MessageDirection.BUYER_TO_SELLER, context.getMessageDirection());
        assertEquals("secretofbuyer", context.getConversation().getBuyerSecret());
        assertEquals("secretofseller", context.getConversation().getSellerSecret());

    }
}
