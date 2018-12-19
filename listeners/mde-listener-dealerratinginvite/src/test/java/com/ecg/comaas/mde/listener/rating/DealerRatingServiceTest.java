package com.ecg.comaas.mde.listener.rating;

import com.ecg.replyts.core.api.model.conversation.Message;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static com.ecg.replyts.core.api.model.conversation.MessageState.SENT;
import static java.lang.String.valueOf;
import static java.util.EnumSet.complementOf;
import static java.util.EnumSet.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public final class DealerRatingServiceTest {

    private static final String CONVERSATION_ID = "conversation_id";
    private static final long DEALER_ID = 123L;

    private final DealerRatingService.DealerRatingServiceClient client = mock(DealerRatingService.DealerRatingServiceClient.class);
    private final Message message = mock(Message.class);
    private DealerRatingService instance = new DealerRatingService(client, true);

    @Test
    public void testMessageWasSent() {
        final Map<String, String> headers = new HashMap<>();
        headers.put("X-Cust-Publisher", "mob-ipad");
        headers.put("X-Cust-Customer_Id", valueOf(DEALER_ID));
        when(message.getCaseInsensitiveHeaders()).thenReturn(headers);
        when(message.getState()).thenReturn(SENT);


        instance.saveInvitation(message, CONVERSATION_ID);

        final ArgumentCaptor<EmailInviteEntity> captor = forClass(EmailInviteEntity.class);

        verify(client).createEmailInvite(captor.capture());

        final EmailInviteEntity entity = captor.getValue();
        assertThat(entity.getDealerId(), is(DEALER_ID));
    }

    @Test
    public void testMessageWasNotSent() {
        complementOf(of(SENT))
                .stream()
                .forEach(state -> {
                    final Map<String, String> headers = new HashMap<>();
                    headers.put("X-Cust-Publisher", "mob-ipad");
                    when(message.getCaseInsensitiveHeaders()).thenReturn(headers);
                    when(message.getState()).thenReturn(state);

                    instance.saveInvitation(message, CONVERSATION_ID);

                    verify(client, never()).createEmailInvite(any());
                });
    }

    @Test
    public void testPluginNotActive() {
        instance = new DealerRatingService(client, false);

        final Map<String, String> headers = new HashMap<>();
        headers.put("X-Cust-Publisher", "mob-ipad");
        when(message.getCaseInsensitiveHeaders()).thenReturn(headers);
        when(message.getState()).thenReturn(SENT);

        instance.saveInvitation(message, CONVERSATION_ID);

        verify(client, never()).createEmailInvite(any());
    }

}