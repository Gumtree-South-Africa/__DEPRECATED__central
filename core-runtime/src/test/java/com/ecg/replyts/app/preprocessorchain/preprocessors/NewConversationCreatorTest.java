package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.cluster.Guids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.MDC;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.CONVERSATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
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

    @After
    public void after()
    {
        MDC.clear();
    }

    @Test
    public void prefersReplyToOverFromHeaderWhenChoosingSender() throws Exception {
        when(mail.getDeliveredTo()).thenReturn("to@host.com");
        when(mail.getFrom()).thenReturn("from@host.com");
        when(mail.getReplyTo()).thenReturn("replyto@host.com");
        creator.setupNewConversation(context);

        assertThat(context.getConversation().getBuyerId()).isEqualTo("replyto@host.com");
    }

    @Test
    public void createsMessageContextProperly() {
        when(mail.getDeliveredTo()).thenReturn("to@host.com");
        when(mail.getFrom()).thenReturn("from@host.com");
        creator.setupNewConversation(context);

        assertThat(context.getConversation().getAdId()).isEqualTo("332211");
        assertThat(context.getMessageDirection()).isEqualTo(MessageDirection.BUYER_TO_SELLER);
        assertThat(context.getConversation().getBuyerSecret()).isEqualTo("secretofbuyer");
        assertThat(context.getConversation().getSellerSecret()).isEqualTo("secretofseller");
        assertThat(MDC.get(CONVERSATION_ID)).isEqualTo("foobar@foobar");
    }
}
